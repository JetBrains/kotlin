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


// TODO consider additional error handling
@Suppress("UNCHECKED_CAST")
val Bindings.kotlinScriptHistory: MutableList<ReplCodeLine>
    get() = getOrPut(KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY, { arrayListOf<ReplCodeLine>() }) as MutableList<ReplCodeLine>

abstract class KotlinJsr223JvmScriptEngineBase(protected val myFactory: ScriptEngineFactory) : AbstractScriptEngine(), ScriptEngine, Compilable {

    protected var codeLineNumber = AtomicInteger(0)

    protected abstract val replCompiler: ReplCompileAction
    protected abstract val replScriptEvaluator: ReplFullEvaluator

    override fun eval(script: String, context: ScriptContext): Any? = compileAndEval(script, context)

    override fun eval(script: Reader, context: ScriptContext): Any? = compileAndEval(script.readText(), context)

    override fun compile(script: String): CompiledScript = compile(script, getContext())

    override fun compile(script: Reader): CompiledScript = compile(script.readText(), getContext())

    override fun createBindings(): Bindings = SimpleBindings()

    override fun getFactory(): ScriptEngineFactory = myFactory

    fun nextCodeLine(code: String) = ReplCodeLine(codeLineNumber.incrementAndGet(), code)

    open fun overrideScriptArgs(context: ScriptContext): ScriptArgsWithTypes? = null

    open fun compileAndEval(script: String, context: ScriptContext): Any? {
        val codeLine = nextCodeLine(script)
        val history = context.getBindings(ScriptContext.ENGINE_SCOPE).kotlinScriptHistory
        val result = replScriptEvaluator.compileAndEval(codeLine, scriptArgs = overrideScriptArgs(context), verifyHistory = history)
        val ret = when (result) {
            is ReplEvalResponse.ValueResult -> result.value
            is ReplEvalResponse.UnitResult -> null
            is ReplEvalResponse.Error -> throw ScriptException(result.message)
            is ReplEvalResponse.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplEvalResponse.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${result.lineNo}")
        }
        context.getBindings(ScriptContext.ENGINE_SCOPE).put(KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY, ArrayList(result.completedEvalHistory))
        return ret
    }

    open fun compile(script: String, context: ScriptContext): CompiledScript {
        val codeLine = nextCodeLine(script)
        val history = context.getBindings(ScriptContext.ENGINE_SCOPE).kotlinScriptHistory

        val result = replCompiler.compile(codeLine, history)
        val compiled = when (result) {
            is ReplCompileResponse.Error -> throw ScriptException("Error${result.locationString()}: ${result.message}")
            is ReplCompileResponse.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplCompileResponse.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${result.lineNo}")
            is ReplCompileResponse.CompiledClasses -> result
        }
        context.getBindings(ScriptContext.ENGINE_SCOPE).put(KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY, ArrayList(result.compiledHistory))
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
            is ReplEvalResponse.ValueResult -> result.value
            is ReplEvalResponse.UnitResult -> null
            is ReplEvalResponse.Error -> throw ScriptException(result.message)
            is ReplEvalResponse.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplEvalResponse.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${result.lineNo}")
        }
        context.getBindings(ScriptContext.ENGINE_SCOPE).put(KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY, ArrayList(result.completedEvalHistory))
        return ret
    }

    class CompiledKotlinScript(val engine: KotlinJsr223JvmScriptEngineBase, val codeLine: ReplCodeLine, val compiledData: ReplCompileResponse.CompiledClasses) : CompiledScript() {
        override fun eval(context: ScriptContext): Any? = engine.eval(this, context)
        override fun getEngine(): ScriptEngine = engine
    }
}

private fun ReplCompileResponse.Error.locationString() =
        if (location == CompilerMessageLocation.NO_LOCATION) ""
        else " at ${location.line}:${location.column}"

