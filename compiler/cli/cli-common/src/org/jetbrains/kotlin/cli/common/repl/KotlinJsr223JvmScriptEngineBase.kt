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
import javax.script.*

val KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY = "kotlin.script.history"

// TODO consider additional error handling
val Bindings.kotlinScriptHistory: MutableList<ReplCodeLine>
    get() = getOrPut(KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY, { arrayListOf<ReplCodeLine>() }) as MutableList<ReplCodeLine>

abstract class KotlinJsr223JvmScriptEngineBase(protected val myFactory: ScriptEngineFactory) : AbstractScriptEngine(), ScriptEngine, Compilable {

    private var lineCount = 0

    protected abstract val replCompiler: ReplCompiler

    protected abstract val replEvaluator: ReplCompiledEvaluator

    override fun eval(script: String, context: ScriptContext): Any? = compile(script, context).eval(context)

    override fun eval(script: Reader, context: ScriptContext): Any? = compile(script.readText(), context).eval()

    override fun compile(script: String): CompiledScript = compile(script, getContext())

    override fun compile(script: Reader): CompiledScript = compile(script.readText(), getContext())

    override fun createBindings(): Bindings = SimpleBindings()

    override fun getFactory(): ScriptEngineFactory = myFactory

    open fun compile(script: String, context: ScriptContext): CompiledScript {
        lineCount += 1

        val codeLine = ReplCodeLine(lineCount, script)
        val history = context.getBindings(ScriptContext.ENGINE_SCOPE).kotlinScriptHistory

        val compileResult = replCompiler.compile(codeLine, history)

        val compiled = when (compileResult) {
            is ReplCompileResult.Error -> throw ScriptException("Error${compileResult.locationString()}: ${compileResult.message}")
            is ReplCompileResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplCompileResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${compileResult.lineNo}")
            is ReplCompileResult.CompiledClasses -> compileResult
        }

        return CompiledKotlinScript(this, codeLine, compiled)
    }

    open fun eval(compiledScript: CompiledKotlinScript, context: ScriptContext): Any? {
        val history = context.getBindings(ScriptContext.ENGINE_SCOPE).kotlinScriptHistory

        val evalResult = try {
            replEvaluator.eval(compiledScript.codeLine, history, compiledScript.compiledData.classes, compiledScript.compiledData.hasResult, compiledScript.compiledData.classpathAddendum)
        }
        catch (e: Exception) {
            throw ScriptException(e)
        }

        val ret = when (evalResult) {
            is ReplEvalResult.ValueResult -> evalResult.value
            is ReplEvalResult.UnitResult -> null
            is ReplEvalResult.Error -> throw ScriptException(evalResult.message)
            is ReplEvalResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplEvalResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${evalResult.lineNo}")
        }
        history.add(compiledScript.codeLine)
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

