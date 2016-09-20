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

package org.jetbrains.kotlin.jsr223

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.repl.GenericReplCompiledEvaluator
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.daemon.client.*
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.Reader
import javax.script.*

class KotlinJvmJsr223ScriptEngine4Idea(
        disposable: Disposable,
        private val factory: ScriptEngineFactory,
        templateClasspath: List<File>,
        templateClassName: String
) : AbstractScriptEngine(), ScriptEngine {

    private val daemon by lazy {
        val path = PathUtil.getKotlinPathsForIdeaPlugin().compilerPath
        assert(path.exists())
        val compilerId = CompilerId.makeCompilerId(path)
        val daemonOptions = configureDaemonOptions()
        val daemonJVMOptions = DaemonJVMOptions()

        val daemonReportMessages = arrayListOf<DaemonReportMessage>()

        KotlinCompilerClient.connectToCompileService(compilerId, daemonJVMOptions, daemonOptions, DaemonReportingTargets(null, daemonReportMessages), true, true)
                ?: throw ScriptException("Unable to connect to repl server:" + daemonReportMessages.joinToString("\n  ", prefix = "\n  ") { "${it.category.name} ${it.message}" })
    }

    private val replCompiler by lazy {
        daemon.let {
            KotlinRemoteReplCompiler(disposable,
                                     it,
                                     makeAutodeletingFlagFile("idea-jsr223-repl-session"),
                                     CompileService.TargetPlatform.JVM,
                                     templateClasspath,
                                     templateClassName,
                                     System.out)
        }
    }

    val localEvaluator by lazy { GenericReplCompiledEvaluator(emptyList(), Thread.currentThread().contextClassLoader) }

    private var lineCount = 0

    private val history = arrayListOf<ReplCodeLine>()

    override fun eval(script: String, context: ScriptContext?): Any? {

        fun ReplCompileResult.Error.locationString() = if (location == CompilerMessageLocation.NO_LOCATION) ""
        else " at ${location.line}:${location.column}:"

        lineCount += 1
        // TODO bind to context
        val codeLine = ReplCodeLine(lineCount, script)
        val compileResult = replCompiler?.compile(codeLine, history)
        val compiled = when (compileResult) {
            is ReplCompileResult.Error -> throw ScriptException("Error${compileResult.locationString()}: ${compileResult.message}")
            is ReplCompileResult.Incomplete -> throw ScriptException("error: incomplete code")
            is ReplCompileResult.HistoryMismatch -> throw ScriptException("Repl history mismatch at line: ${compileResult.lineNo}")
            is ReplCompileResult.CompiledClasses -> compileResult
        }

        val evalResult = localEvaluator.eval(codeLine, history, compiled.classes, compiled.hasResult, compiled.newClasspath)
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

    override fun createBindings(): Bindings = SimpleBindings()

    override fun getFactory(): ScriptEngineFactory = factory
}
