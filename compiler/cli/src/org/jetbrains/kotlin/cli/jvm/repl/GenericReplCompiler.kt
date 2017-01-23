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

package org.jetbrains.kotlin.cli.jvm.repl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptExternalDependencies
import java.io.File

open class GenericReplCompiler(
        disposable: Disposable,
        scriptDefinition: KotlinScriptDefinition,
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector
) : ReplCompiler, GenericReplChecker(disposable, scriptDefinition, compilerConfiguration, messageCollector) {
    private val analyzerEngine = CliReplAnalyzerEngine(environment)

    private var lastDependencies: KotlinScriptExternalDependencies? = null

    private val descriptorsHistory = ReplHistory<ScriptDescriptor>()

    @Synchronized
    override fun compile(codeLine: ReplCodeLine, history: List<ReplCodeLine>): ReplCompileResult {
        checkAndUpdateReplHistoryCollection(descriptorsHistory, history)?.let {
            return@compile ReplCompileResult.HistoryMismatch(descriptorsHistory.lines, it)
        }

        val (psiFile, errorHolder) = run {
            if (lineState == null || lineState!!.codeLine != codeLine) {
                val res = check(codeLine, history)
                when (res) {
                    is ReplCheckResult.Incomplete -> return@compile ReplCompileResult.Incomplete(res.updatedHistory)
                    is ReplCheckResult.Error -> return@compile ReplCompileResult.Error(res.updatedHistory, res.message, res.location)
                    is ReplCheckResult.Ok -> {} // continue
                }
            }
            Pair(lineState!!.psiFile, lineState!!.errorHolder)
        }

        val newDependencies = scriptDefinition.getDependenciesFor(psiFile, environment.project, lastDependencies)
        var classpathAddendum: List<File>? = null
        if (lastDependencies != newDependencies) {
            lastDependencies = newDependencies
            classpathAddendum = newDependencies?.let { environment.updateClasspath(it.classpath.map(::JvmClasspathRoot)) }
        }

        val analysisResult = analyzerEngine.analyzeReplLine(psiFile, codeLine.no)
        AnalyzerWithCompilerReport.Companion.reportDiagnostics(analysisResult.diagnostics, errorHolder)
        val scriptDescriptor = when (analysisResult) {
            is CliReplAnalyzerEngine.ReplLineAnalysisResult.WithErrors -> return ReplCompileResult.Error(descriptorsHistory.lines, errorHolder.renderedDiagnostics)
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
        state.replSpecific.earlierScriptsForReplInterpreter = descriptorsHistory.values
        state.beforeCompile()
        KotlinCodegenFacade.generatePackage(
                state,
                psiFile.script!!.getContainingKtFile().packageFqName,
                setOf(psiFile.script!!.getContainingKtFile()),
                org.jetbrains.kotlin.codegen.CompilationErrorHandler.THROW_EXCEPTION)

        descriptorsHistory.add(codeLine, scriptDescriptor)

        return ReplCompileResult.CompiledClasses(descriptorsHistory.lines,
                                                 state.factory.asList().map { CompiledClassData(it.relativePath, it.asByteArray()) },
                                                 state.replSpecific.hasResult,
                                                 classpathAddendum ?: emptyList())
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }
}
