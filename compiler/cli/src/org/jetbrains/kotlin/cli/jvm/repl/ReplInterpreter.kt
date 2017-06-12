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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import java.io.PrintWriter
import java.net.URLClassLoader
import java.util.concurrent.atomic.AtomicInteger

class ReplInterpreter(
        disposable: Disposable,
        private val configuration: CompilerConfiguration,
        private val replConfiguration: ReplConfiguration
): ReplConfiguration by replConfiguration {

    private val lineNumber = AtomicInteger()

    private val previousIncompleteLines = arrayListOf<String>()
    private val classLoader: ReplClassLoader = run {
        val classpath = configuration.jvmClasspathRoots.map { it.toURI().toURL() }
        ReplClassLoader(URLClassLoader(classpath.toTypedArray(), null))
    }

    private val messageCollector = object : MessageCollector {
        private var hasErrors = false
        private val messageRenderer = MessageRenderer.WITHOUT_PATHS

        override fun clear() {
            hasErrors = false
        }

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
            val msg = messageRenderer.render(severity, message, location).trimEnd()
            with (replConfiguration.writer) {
                when (severity) {
                    CompilerMessageSeverity.EXCEPTION -> sendInternalErrorReport(msg)
                    CompilerMessageSeverity.ERROR -> outputCompileError(msg)
                    CompilerMessageSeverity.STRONG_WARNING -> {} // TODO consider reporting this and two below
                    CompilerMessageSeverity.WARNING -> {}
                    CompilerMessageSeverity.INFO -> {}
                    else -> {}
                }
            }
        }

        override fun hasErrors(): Boolean = hasErrors
    }

    // TODO: add script definition with project-based resolving for IDEA repl
    private val scriptCompiler: ReplCompiler by lazy { GenericReplCompiler(disposable, REPL_LINE_AS_SCRIPT_DEFINITION, configuration, messageCollector) }
    private val scriptEvaluator: ReplFullEvaluator by lazy { GenericReplCompilingEvaluator(scriptCompiler, configuration.jvmClasspathRoots, classLoader, null, ReplRepeatingMode.REPEAT_ANY_PREVIOUS) }

    private val evalState by lazy { scriptEvaluator.createState() }

    fun eval(line: String): ReplEvalResult {

        val fullText = (previousIncompleteLines + line).joinToString(separator = "\n")

        try {

            val evalRes = scriptEvaluator.compileAndEval(evalState, ReplCodeLine(lineNumber.getAndIncrement(), 0, fullText), null, object : InvokeWrapper {
                override fun <T> invoke(body: () -> T): T = executeUserCode { body() }
            })

            when {
                evalRes !is ReplEvalResult.Incomplete -> previousIncompleteLines.clear()
                allowIncompleteLines -> previousIncompleteLines.add(line)
                else -> return ReplEvalResult.Error.CompileTime("incomplete code")
            }
            return evalRes
        }
        catch (e: Throwable) {
            val writer = PrintWriter(System.err)
            classLoader.dumpClasses(writer)
            writer.flush()
            throw e
        }
    }

    private fun <T> executeUserCode(body: () -> T): T {
        try {
            onUserCodeExecuting(true)
            return body()
        }
        finally {
            onUserCodeExecuting(false)
        }
    }

    fun dumpClasses(out: PrintWriter) {
        classLoader.dumpClasses(out)
    }

    companion object {
        private val REPL_LINE_AS_SCRIPT_DEFINITION = object : KotlinScriptDefinition(Any::class) {
            override val name = "Kotlin REPL"
        }
    }
}
