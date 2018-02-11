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
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.KotlinCodegenFacade
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.ScriptDependenciesProvider
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

// WARNING: not thread safe, assuming external synchronization

open class GenericReplCompiler(disposable: Disposable,
                               protected val scriptDefinition: KotlinScriptDefinition,
                               protected val compilerConfiguration: CompilerConfiguration,
                               messageCollector: MessageCollector
) : ReplCompiler {

    constructor(scriptDefinition: KotlinScriptDefinition, compilerConfiguration: CompilerConfiguration, messageCollector: MessageCollector) :
        this(Disposer.newDisposable(), scriptDefinition, compilerConfiguration, messageCollector)

    private val checker = GenericReplChecker(disposable, scriptDefinition, compilerConfiguration, messageCollector)

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = GenericReplCompilerState(checker.environment, lock)

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult = checker.check(state, codeLine)

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult {
        state.lock.write {
            val compilerState = state.asState(GenericReplCompilerState::class.java)

            val (psiFile, errorHolder) = run {
                if (compilerState.lastLineState == null || compilerState.lastLineState!!.codeLine != codeLine) {
                    val res = checker.check(state, codeLine)
                    when (res) {
                        is ReplCheckResult.Incomplete -> return@compile ReplCompileResult.Incomplete()
                        is ReplCheckResult.Error -> return@compile ReplCompileResult.Error(res.message, res.location)
                        is ReplCheckResult.Ok -> {} // continue
                    }
                }
                Pair(compilerState.lastLineState!!.psiFile, compilerState.lastLineState!!.errorHolder)
            }

            val newDependencies = ScriptDependenciesProvider.getInstance(checker.environment.project).getScriptDependencies(psiFile)
            var classpathAddendum: List<File>? = null
            if (compilerState.lastDependencies != newDependencies) {
                compilerState.lastDependencies = newDependencies
                classpathAddendum = newDependencies?.let { checker.environment.updateClasspath(it.classpath.map(::JvmClasspathRoot)) }
            }

            val analysisResult = compilerState.analyzerEngine.analyzeReplLine(psiFile, codeLine)
            AnalyzerWithCompilerReport.reportDiagnostics(analysisResult.diagnostics, errorHolder)
            val scriptDescriptor = when (analysisResult) {
                is ReplCodeAnalyzer.ReplLineAnalysisResult.WithErrors -> return ReplCompileResult.Error(errorHolder.renderedDiagnostics)
                is ReplCodeAnalyzer.ReplLineAnalysisResult.Successful -> analysisResult.scriptDescriptor
                else -> error("Unexpected result ${analysisResult::class.java}")
            }

            val generationState = GenerationState.Builder(
                    psiFile.project,
                    ClassBuilderFactories.binaries(false),
                    compilerState.analyzerEngine.module,
                    compilerState.analyzerEngine.trace.bindingContext,
                    listOf(psiFile),
                    compilerConfiguration
            ).build()
            generationState.replSpecific.scriptResultFieldName = SCRIPT_RESULT_FIELD_NAME
            generationState.replSpecific.earlierScriptsForReplInterpreter = compilerState.history.map { it.item }
            generationState.beforeCompile()
            KotlinCodegenFacade.generatePackage(
                    generationState,
                    psiFile.script!!.containingKtFile.packageFqName,
                    setOf(psiFile.script!!.containingKtFile),
                    org.jetbrains.kotlin.codegen.CompilationErrorHandler.THROW_EXCEPTION)

            val generatedClassname = makeScriptBaseName(codeLine)
            compilerState.history.push(LineId(codeLine), scriptDescriptor)

            val expression = psiFile.getChildOfType<KtScript>()?.
                    getChildOfType<KtBlockExpression>()?.
                    getChildOfType<KtScriptInitializer>()?.
                    getChildOfType<KtExpression>()

            val type = expression?.let {
                compilerState.analyzerEngine.trace.bindingContext.getType(it)
            }?.let {
                DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(it)
            }

            return ReplCompileResult.CompiledClasses(LineId(codeLine),
                                                     compilerState.history.map { it.id },
                                                     generatedClassname,
                                                     generationState.factory.asList().map { CompiledClassData(it.relativePath, it.asByteArray()) },
                                                     generationState.replSpecific.hasResult,
                                                     classpathAddendum ?: emptyList(),
                                                     type)
        }
    }

    companion object {
        private val SCRIPT_RESULT_FIELD_NAME = "\$\$result"
    }
}