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
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

open class GenericReplCompiler(disposable: Disposable,
                               protected val scriptDefinition: KotlinScriptDefinition,
                               protected val compilerConfiguration: CompilerConfiguration,
                               messageCollector: MessageCollector,
                               protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : ReplCompiler {
    private val checker = GenericReplChecker(disposable, scriptDefinition, compilerConfiguration, messageCollector, stateLock)

    private val analyzerEngine = GenericReplAnalyzer(checker.environment, stateLock)

    private var lastDependencies: KotlinScriptExternalDependencies? = null

    private val descriptorsHistory = ReplHistory<ScriptDescriptor>()

    private val generation = AtomicLong(1)

    override fun resetToLine(lineNumber: Int): List<ReplCodeLine> {
        return stateLock.write {
            generation.incrementAndGet()
            val removedCompiledLines = descriptorsHistory.resetToLine(lineNumber)
            val removedAnalyzedLines = analyzerEngine.resetToLine(lineNumber)

            removedCompiledLines.zip(removedAnalyzedLines).forEach {
                if (it.first.first != it.second) {
                    throw IllegalStateException("History mistmatch when resetting lines")
                }
            }

            removedCompiledLines
        }.map { it.first }
    }

    override val history: List<ReplCodeLine> get() = stateLock.read { descriptorsHistory.copySources() }

    override fun check(codeLine: ReplCodeLine): ReplCheckResponse {
        return checker.check(codeLine, generation.get())
    }

    override fun compile(codeLine: ReplCodeLine, verifyHistory: List<ReplCodeLine>?): ReplCompileResponse {
        stateLock.write {
            val firstMismatch = descriptorsHistory.firstMismatchingHistory(verifyHistory)
            if (firstMismatch != null) {
                return@compile ReplCompileResponse.HistoryMismatch(descriptorsHistory.copySources(), firstMismatch)
            }

            val currentGeneration = generation.get()

            val (psiFile, errorHolder) = run {
                if (checker.lineState == null || checker.lineState!!.codeLine != codeLine) {
                    val res = checker.check(codeLine, currentGeneration)
                    when (res) {
                        is ReplCheckResponse.Incomplete -> return@compile ReplCompileResponse.Incomplete(descriptorsHistory.copySources())
                        is ReplCheckResponse.Error -> return@compile ReplCompileResponse.Error(descriptorsHistory.copySources(), res.message, res.location)
                        is ReplCheckResponse.Ok -> NO_ACTION()
                    }
                }
                Pair(checker.lineState!!.psiFile, checker.lineState!!.errorHolder)
            }

            val newDependencies = scriptDefinition.getDependenciesFor(psiFile, checker.environment.project, lastDependencies)
            var classpathAddendum: List<File>? = null
            if (lastDependencies != newDependencies) {
                lastDependencies = newDependencies
                classpathAddendum = newDependencies?.let { checker.environment.updateClasspath(it.classpath.map(::JvmClasspathRoot)) }
            }

            val analysisResult = analyzerEngine.analyzeReplLine(psiFile, codeLine)
            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)
            val scriptDescriptor = when (analysisResult) {
                is GenericReplAnalyzer.ReplLineAnalysisResult.WithErrors -> return ReplCompileResponse.Error(descriptorsHistory.copySources(), errorHolder.renderedDiagnostics)
                is GenericReplAnalyzer.ReplLineAnalysisResult.Successful -> analysisResult.scriptDescriptor
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
            state.replSpecific.earlierScriptsForReplInterpreter = descriptorsHistory.copyValues()
            state.beforeCompile()
            KotlinCodegenFacade.generatePackage(
                    state,
                    psiFile.script!!.getContainingKtFile().packageFqName,
                    setOf(psiFile.script!!.getContainingKtFile()),
                    org.jetbrains.kotlin.codegen.CompilationErrorHandler.THROW_EXCEPTION)

            val generatedClassname = makeSriptBaseName(codeLine, currentGeneration)
            val compiledCodeLine = CompiledReplCodeLine(generatedClassname, codeLine)
            descriptorsHistory.add(compiledCodeLine, scriptDescriptor)

            return ReplCompileResponse.CompiledClasses(descriptorsHistory.copySources(),
                                                       compiledCodeLine,
                                                       generatedClassname,
                                                       state.factory.asList().map { CompiledClassData(it.relativePath, it.asByteArray()) },
                                                       state.replSpecific.hasResult,
                                                       classpathAddendum ?: emptyList())
        }
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }
}