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

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
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
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import org.jetbrains.kotlin.script.ScriptParameter
import java.io.File

private val logger = Logger.getInstance(GenericRepl::class.java)

open class GenericReplChecker(
        disposable: Disposable,
        val scriptDefinition: KotlinScriptDefinition,
        val compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
) : ReplChecker {
    protected val environment = run {
        compilerConfiguration.add(JVMConfigurationKeys.SCRIPT_DEFINITIONS, DelegatingScriptDefWithNoParams(scriptDefinition))
        compilerConfiguration.put<MessageCollector>(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        KotlinCoreEnvironment.createForProduction(disposable, compilerConfiguration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }

    protected val psiFileFactory: PsiFileFactoryImpl = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl

    // "line" - is the unit of evaluation here, could in fact consists of several character lines
    protected class LineState(
            val codeLine: ReplCodeLine,
            val psiFile: KtFile,
            val errorHolder: DiagnosticMessageHolder)

    protected class DelegatingScriptDefWithNoParams(parent: KotlinScriptDefinition) : KotlinScriptDefinition by parent {
        override fun getScriptParameters(scriptDescriptor: ScriptDescriptor): List<ScriptParameter> = emptyList()
    }

    protected var lineState: LineState? = null

    val classpath: MutableList<File> = compilerConfiguration.jvmClasspathRoots.toMutableList()

    fun createDiagnosticHolder() = ReplTerminalDiagnosticMessageHolder()

    override fun check(codeLine: ReplCodeLine, history: Iterable<ReplCodeLine>): ReplCheckResult {
        synchronized(this) {
            val virtualFile =
                    LightVirtualFile("line${codeLine.no}${KotlinParserDefinition.STD_SCRIPT_EXT}", KotlinLanguage.INSTANCE, codeLine.code).apply {
                        charset = CharsetToolkit.UTF8_CHARSET
                    }
            val psiFile: KtFile = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile?
                                  ?: error("Script file not analyzed at line ${codeLine.no}: ${codeLine.code}")

            val errorHolder = createDiagnosticHolder()

            val syntaxErrorReport = AnalyzerWithCompilerReport.Companion.reportSyntaxErrors(psiFile, errorHolder)

            if (!syntaxErrorReport.isHasErrors) {
                lineState = LineState(codeLine, psiFile, errorHolder)
            }

            return when {
                syntaxErrorReport.isHasErrors && syntaxErrorReport.isAllErrorsAtEof -> ReplCheckResult.Incomplete
                syntaxErrorReport.isHasErrors -> ReplCheckResult.Error(errorHolder.renderedDiagnostics)
                else -> ReplCheckResult.Ok
            }
        }
    }
}


abstract class GenericReplCompiler(
        disposable: Disposable,
        scriptDefinition: KotlinScriptDefinition,
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
) : ReplCompiler, GenericReplChecker(disposable, scriptDefinition, compilerConfiguration, messageCollector) {
    private val analyzerEngine = CliReplAnalyzerEngine(environment)

    private var lastDependencies: KotlinScriptExternalDependencies? = null

    private val descriptorsHistory = arrayListOf<Pair<ReplCodeLine, ScriptDescriptor>>()

    override fun compile(codeLine: ReplCodeLine, history: Iterable<ReplCodeLine>): ReplCompileResult {
        synchronized(this) {

            checkAndUpdateReplHistoryCollection(descriptorsHistory, history)?.let {
                return@compile ReplCompileResult.HistoryMismatch(it)
            }

            val (psiFile, errorHolder) = run {
                if (lineState == null || lineState!!.codeLine != codeLine) {
                    val res = check(codeLine, history)
                    when (res) {
                        ReplCheckResult.Incomplete -> return@compile ReplCompileResult.Incomplete
                        is ReplCheckResult.Error -> return@compile ReplCompileResult.Error(res.message)
                        ReplCheckResult.Ok -> {} // continue
                    }
                }
                Pair(lineState!!.psiFile, lineState!!.errorHolder)
            }

            val newDependencies = scriptDefinition.getDependenciesFor(psiFile, environment.project, lastDependencies)
            newDependencies?.let {
                logger.debug("found dependencies: ${it.classpath}")
                val newCp = environment.updateClasspath(it.classpath.map(::JvmClasspathRoot))
                if (newCp != null && newCp.isNotEmpty()) {
                    logger.debug("new dependencies: $newCp")
                    dependenciesAdded(newCp)
                }
            }
            if (lastDependencies != newDependencies) {
                lastDependencies = newDependencies
            }

            val analysisResult = analyzerEngine.analyzeReplLine(psiFile, codeLine.no)
            AnalyzerWithCompilerReport.Companion.reportDiagnostics(analysisResult.diagnostics, errorHolder, false)
            val scriptDescriptor = when (analysisResult) {
                is CliReplAnalyzerEngine.ReplLineAnalysisResult.WithErrors -> return ReplCompileResult.Error(errorHolder.renderedDiagnostics)
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
            state.replSpecific.earlierScriptsForReplInterpreter = descriptorsHistory.map { it.second }
            state.beforeCompile()
            KotlinCodegenFacade.generatePackage(
                    state,
                    psiFile.script!!.getContainingKtFile().packageFqName,
                    setOf(psiFile.script!!.getContainingKtFile()),
                    org.jetbrains.kotlin.codegen.CompilationErrorHandler.THROW_EXCEPTION)

            descriptorsHistory.add(codeLine to scriptDescriptor)

            return ReplCompileResult.CompiledClasses(state.factory.asList().map { CompiledClassData(it.relativePath, it.asByteArray()) }, state.replSpecific.hasResult)
        }
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }
}


open class GenericRepl(
        disposable: Disposable,
        scriptDefinition: KotlinScriptDefinition,
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
) : ReplEvaluator, GenericReplCompiler(disposable, scriptDefinition, compilerConfiguration, messageCollector) {

    private val compiledEvaluator = GenericReplCompiledEvaluator(classpath)

    override fun eval(codeLine: ReplCodeLine, history: Iterable<ReplCodeLine>): ReplEvalResult {
        synchronized(this) {

            val (compiledClasses, hasResult) = compile(codeLine, history).let {
                when (it) {
                    ReplCompileResult.Incomplete -> return@eval ReplEvalResult.Incomplete
                    is ReplCompileResult.HistoryMismatch -> return@eval ReplEvalResult.HistoryMismatch(it.lineNo)
                    is ReplCompileResult.Error -> return@eval ReplEvalResult.Error.CompileTime(it.message)
                    is ReplCompileResult.CompiledClasses -> it.classes to it.hasResult
                }
            }

            return compiledEvaluator.eval(codeLine, history, compiledClasses, hasResult)
        }
    }

    override fun dependenciesAdded(classpath: List<File>) {
        compiledEvaluator.dependenciesAdded(classpath)
    }
}
