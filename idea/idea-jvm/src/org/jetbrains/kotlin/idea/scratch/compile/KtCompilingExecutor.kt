/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.scratch.compile

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.compiler.ex.CompilerPathsEx
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.filterClassFiles
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.debugger.DebuggerUtils
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.scratch.*
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutput
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputType
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import java.io.File

class KtCompilingExecutor(file: ScratchFile) : ScratchExecutor(file) {
    companion object {
        private val TIMEOUT_MS = 30000
    }

    private var backgroundProcessIndicator: ProgressIndicator? = null

    private val expressions = file.getExpressions()

    override fun execute() {
        handler.onStart(file)

        val psiFile = file.getPsiFile() as? KtFile ?: return errorOccurs("Couldn't find KtFile for current editor", isFatal = true)

        if (!checkForErrors(psiFile)) return

        val result = runReadAction { KtScratchSourceFileProcessor().process(expressions) }
        when (result) {
            is KtScratchSourceFileProcessor.Result.Error -> return errorOccurs(result.message, isFatal = true)
            is KtScratchSourceFileProcessor.Result.OK -> {
                LOG.printDebugMessage("After processing by KtScratchSourceFileProcessor:\n ${result.code}")

                object : Task.Backgroundable(psiFile.project, "Running Kotlin Scratch...", true) {
                    override fun run(indicator: ProgressIndicator) {
                        backgroundProcessIndicator = indicator

                        val modifiedScratchSourceFile = runReadAction {
                            KtPsiFactory(psiFile.project).createFileWithLightClassSupport("tmp.kt", result.code, psiFile)
                        }

                        try {
                            val tempDir = DumbService.getInstance(project).runReadActionInSmartMode(
                                Computable {
                                    compileFileToTempDir(modifiedScratchSourceFile)
                                }
                            ) ?: return

                            try {
                                val commandLine = createCommandLine(psiFile, file.getModule(), result.mainClassName, tempDir.path)

                                LOG.printDebugMessage(commandLine.commandLineString)

                                val processHandler = CapturingProcessHandler(commandLine)
                                val executionResult = processHandler.runProcessWithProgressIndicator(indicator, TIMEOUT_MS)
                                when {
                                    executionResult.isTimeout -> {
                                        errorOccurs("Couldn't get scratch execution result - stopped by timeout ($TIMEOUT_MS ms)")
                                    }
                                    executionResult.isCancelled -> {
                                        // ignore
                                    }
                                    else -> {
                                        ProcessOutputParser().parse(executionResult)
                                    }
                                }
                            } finally {
                                tempDir.delete()
                                handler.onFinish(file)
                            }
                        } catch (e: Throwable) {
                            LOG.printDebugMessage(result.code)
                            errorOccurs(e.message ?: "Couldn't compile ${psiFile.name}", e, isFatal = true)
                        }
                    }
                }.queue()
            }
        }
    }

    override fun stop() {
        try {
            backgroundProcessIndicator?.cancel()
        } finally {
            handler.onFinish(file)
        }
    }

    private fun compileFileToTempDir(psiFile: KtFile): File? {
        if (!checkForErrors(psiFile)) return null

        val resolutionFacade = psiFile.getResolutionFacade()
        val (bindingContext, files) = DebuggerUtils.analyzeInlinedFunctions(resolutionFacade, psiFile, false)

        LOG.printDebugMessage("Analyzed files: \n${files.joinToString("\n") { it.virtualFilePath }}")

        val generateClassFilter = object : GenerationState.GenerateClassFilter() {
            override fun shouldGeneratePackagePart(ktFile: KtFile) = ktFile == psiFile
            override fun shouldAnnotateClass(processingClassOrObject: KtClassOrObject) = true
            override fun shouldGenerateClass(processingClassOrObject: KtClassOrObject) = processingClassOrObject.containingKtFile == psiFile
            override fun shouldGenerateScript(script: KtScript) = false
            override fun shouldGenerateCodeFragment(script: KtCodeFragment) = false
        }

        val state = GenerationState.Builder(
            file.project,
            ClassBuilderFactories.BINARIES,
            resolutionFacade.moduleDescriptor,
            bindingContext,
            files,
            CompilerConfiguration.EMPTY
        ).generateDeclaredClassFilter(generateClassFilter).build()

        KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION)

        return writeClassFilesToTempDir(state)
    }

    private fun writeClassFilesToTempDir(state: GenerationState): File {
        val classFiles = state.factory.asList().filterClassFiles()

        val dir = FileUtil.createTempDirectory("compile", "scratch")

        LOG.printDebugMessage("Temp output dir: ${dir.path}")

        for (classFile in classFiles) {
            val tmpOutFile = File(dir, classFile.relativePath)
            tmpOutFile.parentFile.mkdirs()
            tmpOutFile.createNewFile()
            tmpOutFile.writeBytes(classFile.asByteArray())

            LOG.printDebugMessage("Generated class file: ${classFile.relativePath}")
        }
        return dir
    }

    private fun createCommandLine(originalFile: KtFile, module: Module?, mainClassName: String, tempOutDir: String): GeneralCommandLine {
        val javaParameters = KotlinConsoleKeeper.createJavaParametersWithSdk(module)
        javaParameters.mainClass = mainClassName

        javaParameters.classPath.add(tempOutDir)

        if (module != null) {
            val compiledModulePath = CompilerPathsEx.getOutputPaths(arrayOf(module)).toList()
            javaParameters.classPath.addAll(compiledModulePath)

            val moduleDependencies = OrderEnumerator.orderEntries(module).recursively().pathsList.pathList
            javaParameters.classPath.addAll(moduleDependencies)
        }

        val scriptDependencies = ScriptDependenciesManager.getInstance(originalFile.project).getScriptDependencies(originalFile.virtualFile)
        javaParameters.classPath.addAll(scriptDependencies.classpath.map { it.absolutePath })

        return javaParameters.toCommandLine()
    }

    private fun checkForErrors(psiFile: KtFile): Boolean {
        return runReadAction {
            try {
                AnalyzingUtils.checkForSyntacticErrors(psiFile)
            } catch (e: IllegalArgumentException) {
                errorOccurs(e.message ?: "Couldn't compile ${psiFile.name}", isFatal = true)
                return@runReadAction false
            }

            val analysisResult = psiFile.analyzeWithAllCompilerChecks()

            if (analysisResult.isError()) {
                errorOccurs(analysisResult.error.message ?: "Couldn't compile ${psiFile.name}", isFatal = true)
                return@runReadAction false
            }

            val bindingContext = analysisResult.bindingContext
            val diagnostics = bindingContext.diagnostics.filter { it.severity == Severity.ERROR }
            if (diagnostics.isNotEmpty()) {
                val scratchPsiFile = file.getPsiFile()
                diagnostics.forEach { diagnostic ->
                    val errorText = DefaultErrorMessages.render(diagnostic)
                    if (psiFile == scratchPsiFile) {
                        if (diagnostic.psiElement.containingFile == psiFile) {
                            val scratchExpression = expressions.findExpression(diagnostic.psiElement)
                            if (scratchExpression == null) {
                                LOG.error("Couldn't find expression to report error: ${diagnostic.psiElement.getElementTextWithContext()}")
                                handler.error(file, errorText)

                            } else {
                                handler.handle(file, scratchExpression, ScratchOutput(errorText, ScratchOutputType.ERROR))
                            }
                        } else {
                            handler.error(file, errorText)
                        }
                    } else {
                        handler.error(file, errorText)
                    }
                }
                handler.onFinish(file)
                return@runReadAction false
            }
            return@runReadAction true
        }
    }

    private fun List<ScratchExpression>.findExpression(psiElement: PsiElement): ScratchExpression? {
        val elementLine = psiElement.getLineNumber()
        return runReadAction { firstOrNull { elementLine in it.lineStart..it.lineEnd } }
    }

    private fun List<ScratchExpression>.findExpression(lineStart: Int, lineEnd: Int): ScratchExpression? {
        return runReadAction { firstOrNull { it.lineStart == lineStart && it.lineEnd == lineEnd } }
    }

    private inner class ProcessOutputParser {
        fun parse(processOutput: ProcessOutput) {
            val out = processOutput.stdout
            val err = processOutput.stderr
            if (err.isNotBlank()) {
                handler.error(file, err)
            }
            if (out.isNotBlank()) {
                parseStdOut(out)
            }
        }

        private fun parseStdOut(out: String) {
            var results = arrayListOf<String>()
            var userOutput = arrayListOf<String>()
            for (line in out.split("\n")) {
                LOG.printDebugMessage("Compiling executor output: $line")

                if (isOutputEnd(line)) {
                    return
                }

                if (isGeneratedOutput(line)) {
                    val lineWoPrefix = line.removePrefix(KtScratchSourceFileProcessor.GENERATED_OUTPUT_PREFIX)
                    if (isResultEnd(lineWoPrefix)) {
                        val extractedLineInfo = extractLineInfoFrom(lineWoPrefix)
                            ?: return errorOccurs("Couldn't extract line info from line: $lineWoPrefix", isFatal = true)
                        val (startLine, endLine) = extractedLineInfo
                        val scratchExpression = expressions.findExpression(startLine, endLine)
                        if (scratchExpression == null) {
                            LOG.error(
                                "Couldn't find expression with start line = $startLine, end line = $endLine.\n" +
                                        expressions.joinToString("\n")
                            )
                        } else {
                            userOutput.forEach { output ->
                                handler.handle(file, scratchExpression, ScratchOutput(output, ScratchOutputType.OUTPUT))
                            }

                            results.forEach { result ->
                                handler.handle(file, scratchExpression, ScratchOutput(result, ScratchOutputType.RESULT))
                            }
                        }

                        results = arrayListOf()
                        userOutput = arrayListOf()
                    } else if (lineWoPrefix != Unit.toString()) {
                        results.add(lineWoPrefix)
                    }
                } else {
                    userOutput.add(line)
                }
            }
        }

        private fun isOutputEnd(line: String) = line.removeSuffix("\n") == KtScratchSourceFileProcessor.END_OUTPUT_MARKER
        private fun isResultEnd(line: String) = line.startsWith(KtScratchSourceFileProcessor.LINES_INFO_MARKER)
        private fun isGeneratedOutput(line: String) = line.startsWith(KtScratchSourceFileProcessor.GENERATED_OUTPUT_PREFIX)

        private fun extractLineInfoFrom(encoded: String): Pair<Int, Int>? {
            val lineInfo = encoded
                .removePrefix(KtScratchSourceFileProcessor.LINES_INFO_MARKER)
                .removeSuffix("\n")
                .split('|')
            if (lineInfo.size == 2) {
                try {
                    val (a, b) = lineInfo[0].toInt() to lineInfo[1].toInt()
                    if (a > -1 && b > -1) {
                        return a to b
                    }
                } catch (e: NumberFormatException) {
                }
            }
            return null
        }
    }
}