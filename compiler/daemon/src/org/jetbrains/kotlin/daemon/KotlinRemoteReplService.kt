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

package org.jetbrains.kotlin.daemon

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.repl.GenericReplCompiler
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.RemoteOperationsTracer
import org.jetbrains.kotlin.daemon.common.RemoteOutputStream
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.KotlinScriptDefinitionFromAnnotatedTemplate
import org.jetbrains.kotlin.utils.PathUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader

open class KotlinJvmReplService(
        disposable: Disposable,
        templateClasspath: List<File>,
        templateClassName: String,
        protected val fallbackScriptArgs: ScriptArgsWithTypes?,
        compilerOutputStreamProxy: RemoteOutputStream,
        protected val operationsTracer: RemoteOperationsTracer?
) : ReplCompileAction, ReplAtomicEvalAction, ReplCheckAction, ReplEvaluatorExposedInternalHistory {
    protected val compilerMessagesStream = PrintStream(BufferedOutputStream(RemoteOutputStreamClient(compilerOutputStreamProxy, DummyProfiler()), REMOTE_STREAM_BUFFER_SIZE))

    protected class KeepFirstErrorMessageCollector(compilerMessagesStream: PrintStream) : MessageCollector {

        private val innerCollector = PrintingMessageCollector(compilerMessagesStream, MessageRenderer.WITHOUT_PATHS, false)

        internal var firstErrorMessage: String? = null
        internal var firstErrorLocation: CompilerMessageLocation? = null

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation) {
            if (firstErrorMessage == null && severity.isError) {
                firstErrorMessage = message
                firstErrorLocation = location
            }
            innerCollector.report(severity, message, location)
        }

        override fun hasErrors(): Boolean = innerCollector.hasErrors()
        override fun clear() {
            innerCollector.clear()
        }
    }
    protected val messageCollector = KeepFirstErrorMessageCollector(compilerMessagesStream)

    protected val configuration = CompilerConfiguration().apply {
        addJvmClasspathRoots(PathUtil.getJdkClassesRoots())
        addJvmClasspathRoots(PathUtil.getKotlinPathsForCompiler().let { listOf(it.runtimePath, it.reflectPath, it.scriptRuntimePath) })
        addJvmClasspathRoots(templateClasspath)
        put(CommonConfigurationKeys.MODULE_NAME, "kotlin-script")
    }

    protected fun makeScriptDefinition(templateClasspath: List<File>, templateClassName: String): KotlinScriptDefinition? {
        val classloader = URLClassLoader(templateClasspath.map { it.toURI().toURL() }.toTypedArray(), this.javaClass.classLoader)

        try {
            val cls = classloader.loadClass(templateClassName)
            val def = KotlinScriptDefinitionFromAnnotatedTemplate(cls.kotlin, null, null, emptyMap())
            messageCollector.report(
                    CompilerMessageSeverity.INFO,
                    "New script definition $templateClassName: files pattern = \"${def.scriptFilePattern}\", resolver = ${def.resolver?.javaClass?.name}",
                    CompilerMessageLocation.NO_LOCATION
            )
            return def
        }
        catch (ex: ClassNotFoundException) {
            messageCollector.report(
                    CompilerMessageSeverity.ERROR, "Cannot find script definition template class $templateClassName", CompilerMessageLocation.NO_LOCATION
            )
        }
        catch (ex: Exception) {
            messageCollector.report(
                    CompilerMessageSeverity.ERROR, "Error processing script definition template $templateClassName: ${ex.message}", CompilerMessageLocation.NO_LOCATION
            )
        }
        return null
    }

    private val scriptDef = makeScriptDefinition(templateClasspath, templateClassName)

    private val replCompiler: ReplCompiler? by lazy {
        if (scriptDef == null) null
        else GenericReplCompiler(disposable, scriptDef, configuration, messageCollector)
    }

    private val replEvaluator: ReplFullEvaluator? by lazy {
        replCompiler?.let { compiler ->
            GenericReplCompilingEvaluator(compiler, configuration.jvmClasspathRoots, null, fallbackScriptArgs, ReplRepeatingMode.NONE)
        }
    }

    override val lastEvaluatedScripts: List<EvalHistoryType> get() = replEvaluator?.lastEvaluatedScripts ?: emptyList()

    override fun check(codeLine: ReplCodeLine): ReplCheckResponse {
        operationsTracer?.before("check")
        try {
            return replCompiler?.check(codeLine)
                   ?: ReplCheckResponse.Error(messageCollector.firstErrorMessage ?: "Unknown error",
                                            messageCollector.firstErrorLocation ?: CompilerMessageLocation.NO_LOCATION)
        }
        finally {
            operationsTracer?.after("check")
        }
    }

    override fun compile(codeLine: ReplCodeLine, verifyHistory: List<ReplCodeLine>?): ReplCompileResponse {
        operationsTracer?.before("compile")
        try {
            return replCompiler?.compile(codeLine, verifyHistory)
                   ?: ReplCompileResponse.Error(verifyHistory ?: emptyList(),
                                              messageCollector.firstErrorMessage ?: "Unknown error",
                                              messageCollector.firstErrorLocation ?: CompilerMessageLocation.NO_LOCATION)
        }
        finally {
            operationsTracer?.after("compile")
        }
    }

    override fun compileAndEval(codeLine: ReplCodeLine, scriptArgs: ScriptArgsWithTypes?, verifyHistory: List<ReplCodeLine>?, invokeWrapper: InvokeWrapper?): ReplEvalResponse {
        operationsTracer?.before("eval")
        try {
            return replEvaluator?.compileAndEval(codeLine, scriptArgs ?: fallbackScriptArgs, verifyHistory, invokeWrapper)
                   ?: ReplEvalResponse.Error.CompileTime(verifyHistory ?: replEvaluator?.history ?: emptyList(),
                                                         messageCollector.firstErrorMessage ?: "Unknown error",
                                                         messageCollector.firstErrorLocation ?: CompilerMessageLocation.NO_LOCATION)
        }
        finally {
            operationsTracer?.after("eval")
        }
    }
}
