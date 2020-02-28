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

package org.jetbrains.kotlin.cli.common.repl

import java.io.Reader
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.script.*

const val KOTLIN_SCRIPT_STATE_BINDINGS_KEY = "kotlin.script.state"
const val KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY = "kotlin.script.engine"

abstract class KotlinJsr223JvmScriptEngineBase(protected val myFactory: ScriptEngineFactory) : AbstractScriptEngine(), ScriptEngine, Compilable {

    protected abstract val replCompiler: ReplCompilerWithoutCheck
    protected abstract val replEvaluator: ReplFullEvaluator

    override fun eval(script: String, context: ScriptContext): Any? = compileAndEval(script, context)

    override fun eval(script: Reader, context: ScriptContext): Any? = compileAndEval(script.readText(), context)

    override fun compile(script: String): CompiledScript = compile(script, getContext())

    override fun compile(script: Reader): CompiledScript = compile(script.readText(), getContext())

    override fun createBindings(): Bindings = SimpleBindings().apply { put(KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY, this) }

    override fun getFactory(): ScriptEngineFactory = myFactory

    // the parameter could be used in the future when we decide to keep state completely in the context and solve appropriate problems (now e.g. replCompiler keeps separate state)
    fun nextCodeLine(context: ScriptContext, code: String) = getCurrentState(context).let { ReplCodeLine(it.getNextLineNo(), it.currentGeneration, code) }

    protected abstract fun createState(lock: ReentrantReadWriteLock = ReentrantReadWriteLock()): IReplStageState<*>

    protected fun getCurrentState(context: ScriptContext) =
            context.getBindings(ScriptContext.ENGINE_SCOPE)
                    .getOrPut(KOTLIN_SCRIPT_STATE_BINDINGS_KEY, {
                        // TODO: check why createBinding is not called on creating default context, so the engine is not set
                        context.getBindings(ScriptContext.ENGINE_SCOPE).put(KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY, this@KotlinJsr223JvmScriptEngineBase)
                        createState()
                    }) as IReplStageState<*>

    open fun getInvokeWrapper(context: ScriptContext): InvokeWrapper? = null

    open fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = null

    open fun compileAndEval(script: String, context: ScriptContext): Any? {
        val codeLine = nextCodeLine(context, script)
        val state = getCurrentState(context)
        return asJsr223EvalResult {
            replEvaluator.compileAndEval(state, codeLine, overrideScriptArgs(context), getInvokeWrapper(context))
        }
    }

    open fun compile(script: String, context: ScriptContext): CompiledScript {
        val codeLine = nextCodeLine(context, script)
        val state = getCurrentState(context)

        val result = replCompiler.compile(state, codeLine)
        val compiled = when (result) {
            is ReplCompileResult.Error -> throw ScriptException("Error${result.locationString()}: ${result.message}")
            is ReplCompileResult.Incomplete -> throw ScriptException("Error: incomplete code; ${result.message}")
            is ReplCompileResult.CompiledClasses -> result
        }
        return CompiledKotlinScript(this, codeLine, compiled)
    }

    open fun eval(compiledScript: CompiledKotlinScript, context: ScriptContext): Any? {
        val state = getCurrentState(context)
        return asJsr223EvalResult {
            replEvaluator.eval(state, compiledScript.compiledData, overrideScriptArgs(context), getInvokeWrapper(context))
        }
    }

    private fun asJsr223EvalResult(body: () -> ReplEvalResult): Any? {
        val result = try {
            body()
        } catch (e: Exception) {
            throw ScriptException(e)
        }

        return when (result) {
            is ReplEvalResult.ValueResult -> result.value
            is ReplEvalResult.UnitResult -> null
            is ReplEvalResult.Error ->
                when {
                    result is ReplEvalResult.Error.Runtime && result.cause != null ->
                        throw ScriptException(result.cause)
                    result is ReplEvalResult.Error.CompileTime && result.location != null ->
                        throw ScriptException(result.message, result.location.path, result.location.line, result.location.column)
                    else -> throw ScriptException(result.message)
                }
            is ReplEvalResult.Incomplete -> throw ScriptException("Error: incomplete code. ${result.message}")
            is ReplEvalResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${result.lineNo}")
        }
    }

    class CompiledKotlinScript(val engine: KotlinJsr223JvmScriptEngineBase, val codeLine: ReplCodeLine, val compiledData: ReplCompileResult.CompiledClasses) : CompiledScript() {
        override fun eval(context: ScriptContext): Any? = engine.eval(this, context)
        override fun getEngine(): ScriptEngine = engine
    }
}

private fun ReplCompileResult.Error.locationString() =
        if (location == null) ""
        else " at ${location.line}:${location.column}"
