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
import javax.script.*

//val KOTLIN_SCRIPT_HISTORY_BINDINGS_KEY = "kotlin.script.history"
//
//val Bindings.kotlinScriptHistory:

abstract class KotlinJsr223JvmScriptEngineBase(protected val myFactory: ScriptEngineFactory) : AbstractScriptEngine(), ScriptEngine, Compilable {
    protected var lineCount = 0

    protected val history = arrayListOf<ReplCodeLine>()

    abstract fun eval(codeLine: ReplCodeLine, history: List<ReplCodeLine>): ReplEvalResult

    override fun eval(script: String, context: ScriptContext?): Any? {

        lineCount += 1
        // TODO bind to context
        val codeLine = ReplCodeLine(lineCount, script)

        val evalResult = eval(codeLine, history)

        val ret = when (evalResult) {
            is ReplEvalResult.ValueResult -> evalResult.value
            is ReplEvalResult.UnitResult -> null
            is ReplEvalResult.Error -> throw ScriptException(evalResult.message)
            is ReplEvalResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplEvalResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${evalResult.lineNo}")
        }
        history.add(codeLine)
        // TODO update context
        return ret
    }

    override fun eval(script: Reader, context: ScriptContext?): Any? = eval(script.readText(), context)

    override fun compile(p0: String?): CompiledScript {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun compile(p0: Reader?): CompiledScript {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun invokeMethod(p0: Any?, p1: String?, vararg p2: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> getInterface(p0: Class<T>?): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> getInterface(p0: Any?, p1: Class<T>?): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun invokeFunction(p0: String?, vararg p1: Any?): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createBindings(): Bindings = SimpleBindings()

    override fun getFactory(): ScriptEngineFactory = myFactory
}
