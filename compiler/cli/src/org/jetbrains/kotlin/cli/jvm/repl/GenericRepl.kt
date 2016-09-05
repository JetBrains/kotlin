/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli.jvm.repl

import com.google.common.base.Throwables
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.repl.messages.DiagnosticMessageHolder
import org.jetbrains.kotlin.cli.jvm.repl.messages.ReplTerminalDiagnosticMessageHolder
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.ScriptParameter
import java.net.URLClassLoader
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = Logger.getInstance(GenericRepl::class.java)

open class GenericRepl(
        disposable: Disposable,
        val scriptDefinition: KotlinScriptDefinition,
        val compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
) {
    private val environment = run {
        compilerConfiguration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, DelegatingScriptDefWithNoParams(scriptDefinition))
        compilerConfiguration.put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        KotlinCoreEnvironment.createForProduction(disposable, compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    private val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl
    private val analyzerEngine = CliReplAnalyzerEngine(environment)

    private class ChunkState(
            val code: String,
            val psiFile: KtFile,
            val errorHolder: DiagnosticMessageHolder)

    sealed class EvalResult {
        class ValueResult(val value: Any?): EvalResult()

        object UnitResult: EvalResult()
        object Ready: EvalResult()
        object Incomplete : EvalResult()

        sealed class Error(val errorText: String): EvalResult() {
            class Runtime(errorText: String): Error(errorText)
            class CompileTime(errorText: String): Error(errorText)
        }
    }

    private class DelegatingScriptDefWithNoParams(parent: KotlinScriptDefinition) : KotlinScriptDefinition by parent {
        override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> = emptyList()
    }

    private var chunkState: ChunkState? = null

    private var lastDependencies: KotlinScriptExternalDependencies? = null

    val classpath = compilerConfiguration.jvmClasspathRoots.toMutableList()

    private var classLoader: ReplClassLoader =
            ReplClassLoader(URLClassLoader(classpath.map { it.toURI().toURL() }.toTypedArray(), Thread.currentThread().contextClassLoader))
    private val classLoaderLock = ReentrantReadWriteLock()

    private val earlierLines = arrayListOf<EarlierLine>()

    fun createDiagnosticHolder() = ReplTerminalDiagnosticMessageHolder()

    fun checkComplete(executionNumber: Long, code: String): EvalResult {
        synchronized(this) {
            val virtualFile =
                    LightVirtualFile("line$executionNumber${KotlinParserDefinition.STD_SCRIPT_EXT}", KotlinLanguage.INSTANCE, code).apply {
                        charset = CharsetToolkit.UTF8_CHARSET
                    }
            val psiFile: KtFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                                  ?: error("Script file not analyzed at line $executionNumber: $code")

            val errorHolder = createDiagnosticHolder()

            val syntaxErrorReport = AnalyzerWithCompilerReport.Companion.reportSyntaxErrors(psiFile, errorHolder)

            if (!syntaxErrorReport.isHasErrors) {
                chunkState = ChunkState(code, psiFile, errorHolder)
            }

            return when {
                syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof -> EvalResult.Incomplete
                syntaxErrorReport.isHasErrors -> EvalResult.Error.CompileTime(errorHolder.renderedDiagnostics)
                else -> EvalResult.Ready
            }
        }
    }

    fun eval(executionNumber: Long, code: String): EvalResult {
        synchronized(this) {
            val (psiFile, errorHolder) = run {
                if (chunkState == null || chunkState!!.code != code) {
                    val res = checkComplete(executionNumber, code)
                    if (res != EvalResult.Ready) return@eval res
                }
                Pair(chunkState!!.psiFile, chunkState!!.errorHolder)
            }

            val newDependencies = scriptDefinition.getDependenciesFor(psiFile, environment.project, lastDependencies)
            newDependencies?.let {
                logger.debug("found dependencies: ${it.classpath}")
                val newCp = environment.updateClasspath(it.classpath.map(::JvmClasspathRoot))
                if (newCp != null && newCp.isNotEmpty()) {
                    logger.debug("new dependencies: $newCp")
                    classLoaderLock.write {
                        classpath.addAll(newCp)
                        classLoader = ReplClassLoader(URLClassLoader(newCp.map { it.toURI().toURL() }.toTypedArray(), classLoader))
                    }
                }
            }
            if (lastDependencies != newDependencies) {
                lastDependencies = newDependencies
            }

            val analysisResult = analyzerEngine.analyzeReplLine(psiFile, executionNumber.toInt())
            AnalyzerWithCompilerReport.Companion.reportDiagnostics(analysisResult.diagnostics, errorHolder, false)
            val scriptDescriptor = when (analysisResult) {
                is CliReplAnalyzerEngine.ReplLineAnalysisResult.WithErrors -> return EvalResult.Error.CompileTime(errorHolder.renderedDiagnostics)
                is CliReplAnalyzerEngine.ReplLineAnalysisResult.Successful -> analysisResult.scriptDescriptor
                else -> error("Unexpected result ${analysisResult.javaClass}")
            }

            val state = GenerationState(
                    psiFile.project,
                    ClassBuilderFactories.binaries(false),
                    analyzerEngine.module,
                    analyzerEngine.trace.bindingContext,
                    listOf(psiFile),
                    compilerConfiguration
            )
            state.replSpecific.scriptResultFieldName = SCRIPT_RESULT_FIELD_NAME
            state.replSpecific.earlierScriptsForReplInterpreter = earlierLines.map(EarlierLine::getScriptDescriptor)
            state.beforeCompile()
            KotlinCodegenFacade.generatePackage(
                    state,
                    psiFile.script!!.getContainingKtFile().packageFqName,
                    setOf(psiFile.script!!.getContainingKtFile()),
                    org.jetbrains.kotlin.codegen.CompilationErrorHandler.THROW_EXCEPTION)

            for (outputFile in state.factory.asList()) {
                if (outputFile.relativePath.endsWith(".class")) {
                    classLoaderLock.read {
                        classLoader.addClass(JvmClassName.byInternalName(outputFile.relativePath.replaceFirst("\\.class$".toRegex(), "")),
                                             outputFile.asByteArray())
                    }
                }
            }

            try {
                val scriptClass = classLoaderLock.read { classLoader.loadClass("Line$executionNumber") }

                val constructorParams = earlierLines.map(EarlierLine::getScriptClass).toTypedArray()
                val constructorArgs = earlierLines.map(EarlierLine::getScriptInstance).toTypedArray()

                val scriptInstanceConstructor = scriptClass.getConstructor(*constructorParams)
                val scriptInstance =
                        try {
                            evalWithIO { scriptInstanceConstructor.newInstance(*constructorArgs) }
                        }
                        catch (e: Throwable) {
                            // ignore everything in the stack trace until this constructor call
                            return EvalResult.Error.Runtime(renderStackTrace(e.cause!!, startFromMethodName = "${scriptClass.name}.<init>"))
                        }

                val rvField = scriptClass.getDeclaredField(SCRIPT_RESULT_FIELD_NAME).apply { isAccessible = true }
                val rv: Any? = rvField.get(scriptInstance)

                earlierLines.add(EarlierLine(code, scriptDescriptor, scriptClass, scriptInstance))

                return if (state.replSpecific.hasResult) EvalResult.ValueResult(rv) else EvalResult.UnitResult
            }
            catch (e: Throwable) {
                throw e
            }
        }
    }

    // override to capture output
    open fun<T> evalWithIO(body: () -> T): T = body()

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"

        private fun renderStackTrace(cause: Throwable, startFromMethodName: String): String {
            val newTrace = arrayListOf<StackTraceElement>()
            var skip = true
            for ((i, element) in cause.stackTrace.withIndex().reversed()) {
                if ("${element.className}.${element.methodName}" == startFromMethodName) {
                    skip = false
                }
                if (!skip) {
                    newTrace.add(element)
                }
            }

            val resultingTrace = newTrace.reversed().dropLast(1)

            @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UsePropertyAccessSyntax")
            (cause as java.lang.Throwable).setStackTrace(resultingTrace.toTypedArray())

            return Throwables.getStackTraceAsString(cause)
        }
    }
}
