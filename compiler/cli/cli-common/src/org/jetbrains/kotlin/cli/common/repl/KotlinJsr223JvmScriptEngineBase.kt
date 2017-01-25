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

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import java.io.Reader
import java.util.concurrent.atomic.AtomicInteger
import javax.script.*

val KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY = "kotlin.script.history"
val KOTLIN_SCRIPT_LINE_NUMBER_BINDINGS_KEY = "kotlin.script.line.number"

// TODO consider additional error handling
var Bindings.kotlinScriptHistory: MutableList<ReplCodeLine>
    @Suppress("UNCHECKED_CAST")
    get() = getOrPut(KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY, { arrayListOf<ReplCodeLine>() }) as MutableList<ReplCodeLine>
    set(v) { put(KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY, v) }

val Bindings.kotlinScriptLineNumber: AtomicInteger
    @Suppress("UNCHECKED_CAST")
    get() = getOrPut(KOTLIN_SCRIPT_LINE_NUMBER_BINDINGS_KEY, { AtomicInteger(0) }) as AtomicInteger

abstract class KotlinJsr223JvmScriptEngineBase(protected val myFactory: ScriptEngineFactory) : AbstractScriptEngine(), ScriptEngine, Compilable {

    protected abstract val replCompiler: ReplCompileAction
    protected abstract val replScriptEvaluator: ReplFullEvaluator

    override fun eval(script: String, context: ScriptContext): Any? = compileAndEval(script, context)

    override fun eval(script: Reader, context: ScriptContext): Any? = compileAndEval(script.readText(), context)

    override fun compile(script: String): CompiledScript = compile(script, getContext())

    override fun compile(script: Reader): CompiledScript = compile(script.readText(), getContext())

    override fun createBindings(): Bindings = SimpleBindings()

    override fun getFactory(): ScriptEngineFactory = myFactory

    // the parameter could be used in the future when we decide to keep state completely in the context and solve appropriate problems (now e.g. replCompiler keeps separate state)
    fun nextCodeLine(@Suppress("UNUSED_PARAMETER") context: ScriptContext, code: String) = ReplCodeLine(this.context.getBindings(ScriptContext.ENGINE_SCOPE).kotlinScriptLineNumber.incrementAndGet(), code)

    private fun getCurrentHistory(@Suppress("UNUSED_PARAMETER") context: ScriptContext) = this.context.getBindings(ScriptContext.ENGINE_SCOPE).kotlinScriptHistory

    private fun setContextHistory(@Suppress("UNUSED_PARAMETER") context: ScriptContext, history: ArrayList<ReplCodeLine>) {
        this.context.getBindings(ScriptContext.ENGINE_SCOPE).kotlinScriptHistory = history
    }

    open fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = null

    open fun compileAndEval(script: String, context: ScriptContext): Any? {
        val codeLine = nextCodeLine(context, script)
        val history = getCurrentHistory(context)
        val result = replScriptEvaluator.compileAndEval(codeLine, scriptArgs = overrideScriptArgs(context), verifyHistory = history)
        val ret = when (result) {
            is ReplEvalResult.ValueResult -> result.value
            is ReplEvalResult.UnitResult -> null
            is ReplEvalResult.Error -> throw ScriptException(result.message)
            is ReplEvalResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplEvalResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${result.lineNo}")
        }
        setContextHistory(context, ArrayList(result.completedEvalHistory))
        return ret
    }

    open fun compile(script: String, context: ScriptContext): CompiledScript {
        val codeLine = nextCodeLine(context, script)
        val history = getCurrentHistory(context)

        val result = replCompiler.compile(codeLine, history)
        val compiled = when (result) {
            is ReplCompileResult.Error -> throw ScriptException("Error${result.locationString()}: ${result.message}")
            is ReplCompileResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplCompileResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${result.lineNo}")
            is ReplCompileResult.CompiledClasses -> result
        }
        // TODO: check if it is ok to keep compiled history in the same place as compiledEval one
        setContextHistory(context, ArrayList(result.compiledHistory))
        return CompiledKotlinScript(this, codeLine, compiled)
    }

    open fun eval(compiledScript: CompiledKotlinScript, context: ScriptContext): Any? {
        val result = try {
            replScriptEvaluator.eval(compiledScript.compiledData, scriptArgs = overrideScriptArgs(context))
        }
        catch (e: Exception) {
            throw ScriptException(e)
        }

        val ret = when (result) {
            is ReplEvalResult.ValueResult -> result.value
            is ReplEvalResult.UnitResult -> null
            is ReplEvalResult.Error -> throw ScriptException(result.message)
            is ReplEvalResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplEvalResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${result.lineNo}")
        }
        setContextHistory(context, ArrayList(result.completedEvalHistory))
        return ret
    }

    class CompiledKotlinScript(val engine: KotlinJsr223JvmScriptEngineBase, val codeLine: ReplCodeLine, val compiledData: ReplCompileResult.CompiledClasses) : CompiledScript() {
        override fun eval(context: ScriptContext): Any? = engine.eval(this, context)
        override fun getEngine(): ScriptEngine = engine
    }
}

private fun ReplCompileResult.Error.locationString() =
        if (location == CompilerMessageLocation.NO_LOCATION) ""
        else " at ${location.line}:${location.column}"

