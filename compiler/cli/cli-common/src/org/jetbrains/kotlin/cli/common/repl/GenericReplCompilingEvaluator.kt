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

package org.jetbrains.kotlin.cli.common.repl

import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

open class GenericReplCompilingEvaluatorBase(
    val compiler: ReplCompiler,
    val evaluator: ReplEvaluator,
    private val fallbackScriptArgs: ScriptArgsWithTypes? = null
) : ReplFullEvaluator {

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> = AggregatedReplStageState(compiler.createState(lock), evaluator.createState(lock), lock)

    override fun compileAndEval(state: IReplStageState<*>, codeLine: ReplCodeLine, scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResult {
        if (codeLine.code.trim().isEmpty()) {
            return ReplEvalResult.UnitResult()
        }

        return state.lock.write {
            val aggregatedState = state.asState(AggregatedReplStageState::class.java)
            val compiled = compiler.compile(state, codeLine)
            when (compiled) {
                is ReplCompileResult.Error -> {
                    aggregatedState.apply {
                        lock.write {
                            assert(state1.history.size == state2.history.size)
                            adjustHistories() // needed due to statefulness of AnalyzerEngine - in case of compilation errors the line name reuse leads to #KT-17921
                        }
                    }
                    ReplEvalResult.Error.CompileTime(compiled.message, compiled.location)
                }
                is ReplCompileResult.Incomplete -> ReplEvalResult.Incomplete()
                is ReplCompileResult.CompiledClasses -> {
                    val result = eval(state, compiled, scriptArgs, invokeWrapper)
                    when (result) {
                        is ReplEvalResult.Error,
                        is ReplEvalResult.HistoryMismatch,
                        is ReplEvalResult.Incomplete -> {
                            aggregatedState.apply {
                                lock.write {
                                    if (state1.history.size > state2.history.size) {
                                        adjustHistories()
                                        assert(state1.history.size == state2.history.size)
                                    }
                                }
                            }
                            result
                        }
                        is ReplEvalResult.ValueResult,
                        is ReplEvalResult.UnitResult ->
                            result
                    }
                }
            }
        }
    }

    override fun eval(state: IReplStageState<*>, compileResult: ReplCompileResult.CompiledClasses, scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResult =
        evaluator.eval(state, compileResult, scriptArgs, invokeWrapper)

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult = compiler.check(state, codeLine)

    override fun compileToEvaluable(state: IReplStageState<*>, codeLine: ReplCodeLine, defaultScriptArgs: ScriptArgsWithTypes?): Pair<ReplCompileResult, Evaluable?> {
        val compiled = compiler.compile(state, codeLine)
        return when (compiled) {
            // TODO: seems usafe when delayed evaluation may happen after some more compileAndEval calls on the same state; check and fix or protect
            is ReplCompileResult.CompiledClasses -> Pair(compiled, DelayedEvaluation(state, compiled, evaluator, defaultScriptArgs ?: fallbackScriptArgs))
            else -> Pair(compiled, null)
        }
    }

    class DelayedEvaluation(private val state: IReplStageState<*>,
                            override val compiledCode: ReplCompileResult.CompiledClasses,
                            private val evaluator: ReplEvaluator,
                            private val defaultScriptArgs: ScriptArgsWithTypes?) : Evaluable {
        override fun eval(scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResult =
            evaluator.eval(state, compiledCode, scriptArgs ?: defaultScriptArgs, invokeWrapper)
    }
}

class GenericReplCompilingEvaluator(
    compiler: ReplCompiler,
    baseClasspath: Iterable<File>,
    baseClassloader: ClassLoader? = Thread.currentThread().contextClassLoader,
    fallbackScriptArgs: ScriptArgsWithTypes? = null,
    repeatingMode: ReplRepeatingMode = ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT
) : GenericReplCompilingEvaluatorBase(
    compiler,
    GenericReplEvaluator(baseClasspath, baseClassloader, fallbackScriptArgs, repeatingMode),
    fallbackScriptArgs
)

private fun AggregatedReplStageState<*, *>.adjustHistories(): Iterable<ILineId>? =
    state2.history.peek()?.let {
        state1.history.resetTo(it.id)
    }
        ?: state1.history.reset()
