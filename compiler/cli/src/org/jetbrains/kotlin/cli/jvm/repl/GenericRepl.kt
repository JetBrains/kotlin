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
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.util.concurrent.locks.ReentrantReadWriteLock

// TODO: remove if unused after switching old repl to the new infrastruct
open class GenericRepl protected constructor(
        disposable: Disposable,
        scriptDefinition: KotlinScriptDefinition,
        compilerConfiguration: CompilerConfiguration,
        messageCollector: MessageCollector,
        baseClassloader: ClassLoader?,
        protected val fallbackScriptArgs: ScriptArgsWithTypes? = null,
        protected val repeatingMode: ReplRepeatingMode = ReplRepeatingMode.NONE
) : ReplCompiler, ReplEvaluator, ReplAtomicEvaluator {

    protected val compiler: ReplCompiler by lazy { GenericReplCompiler(disposable, scriptDefinition, compilerConfiguration, messageCollector) }
    protected val evaluator: ReplFullEvaluator by lazy { GenericReplCompilingEvaluator(compiler, compilerConfiguration.jvmClasspathRoots, baseClassloader, fallbackScriptArgs, repeatingMode) }

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = AggregatedReplStageState(compiler.createState(lock), evaluator.createState(lock), lock)

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult = compiler.check(state, codeLine)

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult = compiler.compile(state, codeLine)

    override fun eval(state: IReplStageState<*>, compileResult: ReplCompileResult.CompiledClasses, scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResult =
            evaluator.eval(state, compileResult, scriptArgs, invokeWrapper)

    override fun compileAndEval(state: IReplStageState<*>, codeLine: ReplCodeLine, scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResult =
            evaluator.compileAndEval(state, codeLine, scriptArgs, invokeWrapper)
}