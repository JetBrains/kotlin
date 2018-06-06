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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.compiler.ex.CompilerPathsEx
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderEnumerator
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
import org.jetbrains.kotlin.idea.debugger.DebuggerUtils
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.scratch.*
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutput
import org.jetbrains.kotlin.idea.scratch.output.ScratchOutputType
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import java.io.File

class KtCompilingExecutor(file: ScratchFile) : ScratchExecutor(file) {
    override fun execute() {
        val module = file.getModule() ?: return error("Module should be selected")
        val psiFile = file.getPsiFile() as? KtFile ?: return error("Couldn't find KtFile for current editor")

        if (!checkForErrors(psiFile)) {
            return error("Compilation Error")
        }

        val result = KtScratchSourceFileProcessor().process(file)
        when (result) {
            is KtScratchSourceFileProcessor.Result.Error -> return error(result.message)
            is KtScratchSourceFileProcessor.Result.OK -> {
                ApplicationManager.getApplication().invokeLater {
                    LOG.printDebugMessage("After processing by KtScratchSourceFileProcessor:\n ${result.code}")

                    val modifiedScratchSourceFile =
                        KtPsiFactory(psiFile.project).createFileWithLightClassSupport("tmp.kt", result.code, psiFile)

                    try {
                        val tempDir = compileFileToTempDir(modifiedScratchSourceFile) ?: return@invokeLater

                        try {
                            val commandLine = createCommandLine(module, result.mainClassName, tempDir.path)

                            LOG.printDebugMessage(commandLine.commandLineString)

                            val handler = CapturingProcessHandler(commandLine)
                            ProcessOutputParser().parse(handler.runProcess())
                        } finally {
                            tempDir.delete()
                        }
                    } catch (e: Throwable) {
                        LOG.info(result.code, e)
                        handlers.forEach { it.error(file, e.message ?: "Couldn't compile ${psiFile.name}") }
                    } finally {
                        handlers.forEach { it.onFinish(file) }
                    }
                }
            }
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

    private fun createCommandLine(module: Module, mainClassName: String, tempOutDir: String): GeneralCommandLine {
        val javaParameters = KotlinConsoleKeeper.createJavaParametersWithSdk(module)
        javaParameters.mainClass = mainClassName

        val compiledModulePath = CompilerPathsEx.getOutputPaths(arrayOf(module)).toList()
        val moduleDependencies = OrderEnumerator.orderEntries(module).recursively().pathsList.pathList

        javaParameters.classPath.add(tempOutDir)
        javaParameters.classPath.addAll(compiledModulePath)
        javaParameters.classPath.addAll(moduleDependencies)

        return javaParameters.toCommandLine()
    }

    private fun checkForErrors(psiFile: KtFile): Boolean {
        return runReadAction {
            try {
                AnalyzingUtils.checkForSyntacticErrors(psiFile)
            } catch (e: IllegalArgumentException) {
                handlers.forEach { it.error(file, e.message ?: "Couldn't compile ${psiFile.name}") }
                return@runReadAction false
            }

            val analysisResult = psiFile.analyzeWithAllCompilerChecks()

            if (analysisResult.isError()) {
                handlers.forEach { it.error(file, analysisResult.error.message ?: "Couldn't compile ${psiFile.name}") }
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
                            val scratchExpression = file.findExpression(diagnostic.psiElement)
                            if (scratchExpression == null) {
                                LOG.error("Couldn't find expression to report error: ${diagnostic.psiElement.getElementTextWithContext()}")
                                handlers.forEach { it.error(file, errorText) }

                            } else {
                                handlers.forEach { it.handle(file, scratchExpression, ScratchOutput(errorText, ScratchOutputType.ERROR)) }
                            }
                        } else {
                            handlers.forEach { it.error(file, errorText) }
                        }
                    } else {
                        handlers.forEach { it.error(file, errorText) }
                    }
                }
                return@runReadAction false
            }
            return@runReadAction true
        }
    }

    private fun error(message: String) {
        handlers.forEach { it.error(file, message) }
        handlers.forEach { it.onFinish(file) }
    }

    private fun ScratchFile.findExpression(psiElement: PsiElement): ScratchExpression? {
        val elementLine = psiElement.getLineNumber()
        return getExpressions().firstOrNull { elementLine in it.lineStart..it.lineEnd }
    }

    private fun ScratchFile.findExpression(lineStart: Int, lineEnd: Int): ScratchExpression? {
        return getExpressions().firstOrNull { it.lineStart == lineStart && it.lineEnd == lineEnd }
    }

    private inner class ProcessOutputParser {
        fun parse(processOutput: ProcessOutput) {
            val out = processOutput.stdout
            val err = processOutput.stderr
            if (err.isNotBlank()) {
                handlers.forEach { it.error(file, err) }
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
                                ?: return error("Couldn't extract line info from line: $lineWoPrefix")
                        val (startLine, endLine) = extractedLineInfo
                        val scratchExpression = file.findExpression(startLine, endLine)
                        if (scratchExpression == null) {
                            LOG.error(
                                "Couldn't find expression with start line = $startLine, end line = $endLine.\n" +
                                        file.getExpressions().joinToString("\n")
                            )
                        } else {
                            userOutput.forEach { output ->
                                handlers.forEach {
                                    it.handle(file, scratchExpression, ScratchOutput(output, ScratchOutputType.OUTPUT))
                                }
                            }

                            results.forEach { result ->
                                handlers.forEach {
                                    it.handle(file, scratchExpression, ScratchOutput(result, ScratchOutputType.RESULT))
                                }
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