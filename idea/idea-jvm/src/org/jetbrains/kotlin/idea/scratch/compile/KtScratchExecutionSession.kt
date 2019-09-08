/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch.compile

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.filterClassFiles
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.debugger.DebuggerUtils
import org.jetbrains.kotlin.idea.scratch.LOG
import org.jetbrains.kotlin.idea.scratch.ScratchExpression
import org.jetbrains.kotlin.idea.scratch.ScratchFile
import org.jetbrains.kotlin.idea.scratch.printDebugMessage
import org.jetbrains.kotlin.idea.util.JavaParametersBuilder
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import java.io.File
import kotlin.script.experimental.api.valueOrNull

class KtScratchExecutionSession(
    private val file: ScratchFile,
    private val executor: KtCompilingExecutor
) {
    companion object {
        private val TIMEOUT_MS = 30000
    }

    private var backgroundProcessIndicator: ProgressIndicator? = null

    fun execute(callback: () -> Unit) {
        val psiFile = file.getPsiFile() as? KtFile ?: return executor.errorOccurs("Couldn't find KtFile for current editor", isFatal = true)

        val expressions = file.getExpressions()
        if (!executor.checkForErrors(psiFile, expressions)) return

        val result = runReadAction { KtScratchSourceFileProcessor().process(expressions) }
        when (result) {
            is KtScratchSourceFileProcessor.Result.Error -> return executor.errorOccurs(result.message, isFatal = true)
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
                                    compileFileToTempDir(modifiedScratchSourceFile, expressions)
                                }
                            ) ?: return

                            try {
                                val commandLine = createCommandLine(psiFile, file.module, result.mainClassName, tempDir.path)

                                LOG.printDebugMessage(commandLine.commandLineString)

                                val processHandler = CapturingProcessHandler(commandLine)
                                val executionResult = processHandler.runProcessWithProgressIndicator(indicator, TIMEOUT_MS)
                                when {
                                    executionResult.isTimeout -> {
                                        executor.errorOccurs("Couldn't get scratch execution result - stopped by timeout ($TIMEOUT_MS ms)")
                                    }
                                    executionResult.isCancelled -> {
                                        // ignore
                                    }
                                    else -> {
                                        executor.parseOutput(executionResult, expressions)
                                    }
                                }
                            } finally {
                                tempDir.delete()
                                callback()
                            }
                        } catch (e: Throwable) {
                            LOG.printDebugMessage(result.code)
                            executor.errorOccurs(e.message ?: "Couldn't compile ${psiFile.name}", e, isFatal = true)
                        }
                    }
                }.queue()
            }
        }
    }

    fun stop() {
        backgroundProcessIndicator?.cancel()
    }

    private fun compileFileToTempDir(psiFile: KtFile, expressions: List<ScratchExpression>): File? {
        if (!executor.checkForErrors(psiFile, expressions)) return null

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
        val javaParameters = JavaParametersBuilder(originalFile.project)
            .withSdkFrom(module, true)
            .withMainClassName(mainClassName)
            .build()

        javaParameters.classPath.add(tempOutDir)

        if (module != null) {
            javaParameters.classPath.addAll(JavaParametersBuilder.getModuleDependencies(module))
        }

        ScriptDependenciesManager.getInstance(originalFile.project)
            .getRefinedCompilationConfiguration(originalFile.virtualFile)?.valueOrNull()?.let {
                javaParameters.classPath.addAll(it.dependenciesClassPath.map { f -> f.absolutePath })
            }

        return javaParameters.toCommandLine()
    }
}