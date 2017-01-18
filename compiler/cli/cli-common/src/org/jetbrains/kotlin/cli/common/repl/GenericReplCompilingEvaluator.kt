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

class GenericReplCompilingEvaluator(val compiler: ReplCompiler,
                                    baseClasspath: Iterable<File>,
                                    baseClassloader: ClassLoader?,
                                    protected val fallbackScriptArgs: ScriptArgsWithTypes? = null,
                                    repeatingMode: ReplRepeatingMode = ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT,
                                    protected val stateLock: ReentrantReadWriteLock = ReentrantReadWriteLock()) : ReplFullEvaluator {
    val evaluator = GenericReplEvaluator(baseClasspath, baseClassloader, fallbackScriptArgs, repeatingMode, stateLock)

    override fun compileAndEval(codeLine: ReplCodeLine, scriptArgs: ScriptArgsWithTypes?, verifyHistory: List<ReplCodeLine>?, invokeWrapper: InvokeWrapper?): ReplEvalResponse {
        return stateLock.write {
            @Suppress("DEPRECATION")
            val compiled = compiler.compile(codeLine, verifyHistory)
            when (compiled) {
                is ReplCompileResponse.Error -> ReplEvalResponse.Error.CompileTime(compiled.compiledHistory, compiled.message, compiled.location)
                is ReplCompileResponse.HistoryMismatch -> ReplEvalResponse.HistoryMismatch(compiled.compiledHistory, compiled.lineNo)
                is ReplCompileResponse.Incomplete -> ReplEvalResponse.Incomplete(compiled.compiledHistory)
                is ReplCompileResponse.CompiledClasses -> {
                    @Suppress("DEPRECATION")
                    val result = eval(compiled, scriptArgs, invokeWrapper)
                    when (result) {
                        is ReplEvalResponse.Error,
                        is ReplEvalResponse.HistoryMismatch,
                        is ReplEvalResponse.Incomplete -> {
                            result.completedEvalHistory.lastOrNull()?.let { compiler.resetToLine(it) }
                            result
                        }
                        is ReplEvalResponse.ValueResult,
                        is ReplEvalResponse.UnitResult ->
                            result
                        else -> throw IllegalStateException("Unknown evaluator result type ${compiled}")
                    }
                }
                else -> throw IllegalStateException("Unknown compiler result type ${compiled}")
            }
        }
    }

    override fun resetToLine(lineNumber: Int): List<ReplCodeLine> {
        stateLock.write {
            val removedCompiledLines = compiler.resetToLine(lineNumber)
            val removedEvaluatorLines = evaluator.resetToLine(lineNumber)

            removedCompiledLines.zip(removedEvaluatorLines).forEach {
                if (it.first != it.second) {
                    throw IllegalStateException("History mistmatch when resetting lines")
                }
            }

            return removedCompiledLines
        }
    }

    override fun resetToLine(line: ReplCodeLine): List<ReplCodeLine> = resetToLine(line.no)


    override val lastEvaluatedScripts: List<EvalHistoryType> get() = evaluator.lastEvaluatedScripts
    override val history: List<ReplCodeLine> get() = evaluator.history
    override val currentClasspath: List<File> get() = evaluator.currentClasspath

    override val compiledHistory: List<ReplCodeLine> get() = compiler.history
    override val evaluatedHistory: List<ReplCodeLine> get() = evaluator.history

    override fun eval(compileResult: ReplCompileResponse.CompiledClasses, scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResponse {
        return evaluator.eval(compileResult, scriptArgs, invokeWrapper)
    }

    override fun check(codeLine: ReplCodeLine): ReplCheckResponse {
        return compiler.check(codeLine)
    }

    override fun compileToEvaluable(codeLine: ReplCodeLine, defaultScriptArgs: ScriptArgsWithTypes?, verifyHistory: List<ReplCodeLine>?): Pair<ReplCompileResponse, Evaluable?> {
        val compiled = compiler.compile(codeLine, verifyHistory)
        return if (compiled is ReplCompileResponse.CompiledClasses) {
            Pair(compiled, DelayedEvaluation(compiled, stateLock, evaluator, defaultScriptArgs ?: fallbackScriptArgs))
        }
        else {
            Pair(compiled, null)
        }
    }

    class DelayedEvaluation(override val compiledCode: ReplCompileResponse.CompiledClasses,
                            private val stateLock: ReentrantReadWriteLock,
                            private val evaluator: ReplEvaluator,
                            private val defaultScriptArgs: ScriptArgsWithTypes?) : Evaluable {
        override fun eval(scriptArgs: ScriptArgsWithTypes?, invokeWrapper: InvokeWrapper?): ReplEvalResponse {
            return stateLock.write { evaluator.eval(compiledCode, scriptArgs ?: defaultScriptArgs, invokeWrapper) }
        }
    }
}