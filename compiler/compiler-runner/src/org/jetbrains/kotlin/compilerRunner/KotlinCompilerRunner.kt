/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.jps.api.GlobalOptions
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.mergeBeans
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.daemon.client.CompilationServices
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit

object KotlinCompilerRunner {
    private val K2JVM_COMPILER = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
    private val K2JS_COMPILER = "org.jetbrains.kotlin.cli.js.K2JSCompiler"
    private val INTERNAL_ERROR = ExitCode.INTERNAL_ERROR.toString()

    fun runK2JvmCompiler(
            commonArguments: CommonCompilerArguments,
            k2jvmArguments: K2JVMCompilerArguments,
            compilerSettings: CompilerSettings,
            messageCollector: MessageCollector,
            environment: CompilerEnvironment,
            moduleFile: File,
            collector: OutputItemsCollector) {
        val arguments = mergeBeans(commonArguments, k2jvmArguments)
        setupK2JvmArguments(moduleFile, arguments)

        runCompiler(K2JVM_COMPILER, arguments, compilerSettings.additionalArguments, messageCollector, collector, environment)
    }

    fun runK2JsCompiler(
            commonArguments: CommonCompilerArguments,
            k2jsArguments: K2JSCompilerArguments,
            compilerSettings: CompilerSettings,
            messageCollector: MessageCollector,
            environment: CompilerEnvironment,
            collector: OutputItemsCollector,
            sourceFiles: Collection<File>,
            libraryFiles: List<String>,
            outputFile: File) {
        val arguments = mergeBeans(commonArguments, k2jsArguments)
        setupK2JsArguments(outputFile, sourceFiles, libraryFiles, arguments)

        runCompiler(K2JS_COMPILER, arguments, compilerSettings.additionalArguments, messageCollector, collector, environment)
    }

    private fun processCompilerOutput(
            messageCollector: MessageCollector,
            collector: OutputItemsCollector,
            stream: ByteArrayOutputStream,
            exitCode: String) {
        val reader = BufferedReader(StringReader(stream.toString()))
        CompilerOutputParser.parseCompilerMessagesFromReader(messageCollector, reader, collector)

        if (INTERNAL_ERROR == exitCode) {
            reportInternalCompilerError(messageCollector)
        }
    }

    private fun reportInternalCompilerError(messageCollector: MessageCollector) {
        messageCollector.report(ERROR, "Compiler terminated with internal error", CompilerMessageLocation.NO_LOCATION)
    }

    private fun runCompiler(
            compilerClassName: String,
            arguments: CommonCompilerArguments,
            additionalArguments: String,
            messageCollector: MessageCollector,
            collector: OutputItemsCollector,
            environment: CompilerEnvironment) {
        try {
            messageCollector.report(INFO, "Using kotlin-home = " + environment.kotlinPaths.homePath, CompilerMessageLocation.NO_LOCATION)

            val argumentsList = ArgumentUtils.convertArgumentsToStringList(arguments)
            argumentsList.addAll(additionalArguments.split(" "))

            val argsArray = argumentsList.toTypedArray()

            if (!tryCompileWithDaemon(compilerClassName, argsArray, environment, messageCollector, collector)) {
                // otherwise fallback to in-process
                KotlinBuilder.LOG.info("Compile in-process")

                val stream = ByteArrayOutputStream()
                val out = PrintStream(stream)

                // the property should be set at least for parallel builds to avoid parallel building problems (racing between destroying and using environment)
                // unfortunately it cannot be currently set by default globally, because it breaks many tests
                // since there is no reliable way so far to detect running under tests, switching it on only for parallel builds
                if (System.getProperty(GlobalOptions.COMPILE_PARALLEL_OPTION, "false").toBoolean())
                    System.setProperty(KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY, "true")

                val rc = CompilerRunnerUtil.invokeExecMethod(compilerClassName, argsArray, environment, messageCollector, out)

                // exec() returns an ExitCode object, class of which is loaded with a different class loader,
                // so we take it's contents through reflection
                processCompilerOutput(messageCollector, collector, stream, getReturnCodeFromObject(rc))
            }
        }
        catch (e: Throwable) {
            MessageCollectorUtil.reportException(messageCollector, e)
            reportInternalCompilerError(messageCollector)
        }

    }

    internal class DaemonConnection(val daemon: CompileService?, val sessionId: Int = CompileService.NO_SESSION)

    internal object getDaemonConnection {
        private @Volatile var connection: DaemonConnection? = null

        @Synchronized operator fun invoke(environment: CompilerEnvironment, messageCollector: MessageCollector): DaemonConnection {
            if (connection == null) {
                val libPath = CompilerRunnerUtil.getLibPath(environment.kotlinPaths, messageCollector)
                val compilerId = CompilerId.makeCompilerId(File(libPath, "kotlin-compiler.jar"))
                val daemonOptions = configureDaemonOptions()
                val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = true, inheritAdditionalProperties = true)

                val daemonReportMessages = ArrayList<DaemonReportMessage>()

                val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

                profiler.withMeasure(null) {
                    fun newFlagFile(): File {
                        val flagFile = File.createTempFile("kotlin-compiler-jps-session-", "-is-running")
                        flagFile.deleteOnExit()
                        return flagFile
                    }
                    val daemon = KotlinCompilerClient.connectToCompileService(compilerId, daemonJVMOptions, daemonOptions, DaemonReportingTargets(null, daemonReportMessages), true, true)
                    connection = DaemonConnection(daemon, daemon?.leaseCompileSession(newFlagFile().absolutePath)?.get() ?: CompileService.NO_SESSION)
                }

                for (msg in daemonReportMessages) {
                    messageCollector.report(CompilerMessageSeverity.INFO,
                                            (if (msg.category == DaemonReportCategory.EXCEPTION && connection?.daemon == null)  "Falling  back to compilation without daemon due to error: " else "") + msg.message,
                                            CompilerMessageLocation.NO_LOCATION)
                }

                reportTotalAndThreadPerf("Daemon connect", daemonOptions, messageCollector, profiler)
            }
            return connection!!
        }
    }

    private fun tryCompileWithDaemon(compilerClassName: String,
                                     argsArray: Array<String>,
                                     environment: CompilerEnvironment,
                                     messageCollector: MessageCollector,
                                     collector: OutputItemsCollector,
                                     retryOnConnectionError: Boolean = true): Boolean {

        if (isDaemonEnabled()) {

            KotlinBuilder.LOG.debug("Try to connect to daemon")
            val connection = getDaemonConnection(environment, messageCollector)

            if (connection.daemon != null) {
                KotlinBuilder.LOG.info("Connected to daemon")

                val compilerOut = ByteArrayOutputStream()
                val daemonOut = ByteArrayOutputStream()

                val services = CompilationServices(
                        incrementalCompilationComponents = environment.services.get(IncrementalCompilationComponents::class.java),
                        compilationCanceledStatus = environment.services.get(CompilationCanceledStatus::class.java))

                val targetPlatform = when (compilerClassName) {
                    K2JVM_COMPILER -> CompileService.TargetPlatform.JVM
                    K2JS_COMPILER -> CompileService.TargetPlatform.JS
                    else -> throw IllegalArgumentException("Unknown compiler type $compilerClassName")
                }

                fun retryOrFalse(e: Exception): Boolean {
                    if (retryOnConnectionError) {
                        KotlinBuilder.LOG.debug("retrying once on daemon connection error: ${e.message}")
                        return tryCompileWithDaemon(compilerClassName, argsArray, environment, messageCollector, collector, retryOnConnectionError = false)
                    }
                    KotlinBuilder.LOG.info("daemon connection error: ${e.message}")
                    return false
                }

                val res: Int = try {
                    KotlinCompilerClient.incrementalCompile(connection.daemon, connection.sessionId, targetPlatform, argsArray, services, compilerOut, daemonOut)
                }
                catch (e: java.rmi.ConnectException) {
                    return retryOrFalse(e)
                }
                catch (e: java.rmi.UnmarshalException) {
                    return retryOrFalse(e)
                }

                processCompilerOutput(messageCollector, collector, compilerOut, res.toString())
                BufferedReader(StringReader(daemonOut.toString())).forEachLine {
                    messageCollector.report(CompilerMessageSeverity.INFO, it, CompilerMessageLocation.NO_LOCATION)
                }
                return true
            }

            KotlinBuilder.LOG.info("Daemon not found")
        }
        return false
    }

    private fun reportTotalAndThreadPerf(message: String, daemonOptions: DaemonOptions, messageCollector: MessageCollector, profiler: Profiler) {
        if (daemonOptions.reportPerf) {
            fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
            val counters = profiler.getTotalCounters()
            messageCollector.report(INFO,
                                    "PERF: $message ${counters.time.ms()} ms, thread ${counters.threadTime.ms()}",
                                    CompilerMessageLocation.NO_LOCATION)
        }
    }

    private fun getReturnCodeFromObject(rc: Any?): String {
        when {
            rc == null -> return INTERNAL_ERROR
            ExitCode::class.java.name == rc.javaClass.name -> return rc.toString()
            else -> throw IllegalStateException("Unexpected return: " + rc)
        }
    }

    private fun setupK2JvmArguments(moduleFile: File, settings: K2JVMCompilerArguments) {
        with(settings) {
            module = moduleFile.absolutePath
            noStdlib = true
            noReflect = true
            noJdk = true
        }
    }

    private fun setupK2JsArguments( _outputFile: File, sourceFiles: Collection<File>, _libraryFiles: List<String>, settings: K2JSCompilerArguments) {
        with(settings) {
            noStdlib = true
            freeArgs = sourceFiles.map { it.path }
            outputFile = _outputFile.path
            metaInfo = true
            libraryFiles = _libraryFiles.toTypedArray()
        }
    }
}
