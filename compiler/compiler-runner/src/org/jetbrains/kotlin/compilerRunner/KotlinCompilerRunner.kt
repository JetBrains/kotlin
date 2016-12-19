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

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.daemon.client.CompilationServices
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.util.*
import java.util.concurrent.TimeUnit

interface KotlinLogger {
    fun error(msg: String)
    fun warn(msg: String)
    fun info(msg: String)
    fun debug(msg: String)
}

abstract class KotlinCompilerRunner<in Env : CompilerEnvironment> {
    protected val K2JVM_COMPILER = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
    protected val K2JS_COMPILER = "org.jetbrains.kotlin.cli.js.K2JSCompiler"
    protected val INTERNAL_ERROR = ExitCode.INTERNAL_ERROR.toString()

    protected abstract val log: KotlinLogger

    class DaemonConnection(val daemon: CompileService?, val sessionId: Int = CompileService.NO_SESSION)

    protected abstract fun getDaemonConnection(environment: Env): DaemonConnection

    @Synchronized
    protected fun newDaemonConnection(compilerId: CompilerId, flagFile: File, environment: Env): DaemonConnection {
        val daemonOptions = configureDaemonOptions()
        val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = true, inheritAdditionalProperties = true)

        val daemonReportMessages = ArrayList<DaemonReportMessage>()

        val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

        val connection = profiler.withMeasure(null) {
            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, daemonJVMOptions, daemonOptions, DaemonReportingTargets(null, daemonReportMessages), true, true)
            DaemonConnection(daemon, daemon?.leaseCompileSession(flagFile.absolutePath)?.get() ?: CompileService.NO_SESSION)
        }

        for (msg in daemonReportMessages) {
            environment.messageCollector.report(CompilerMessageSeverity.INFO,
                                    (if (msg.category == DaemonReportCategory.EXCEPTION && connection.daemon == null) "Falling  back to compilation without daemon due to error: " else "") + msg.message,
                                                CompilerMessageLocation.NO_LOCATION)
        }

        fun reportTotalAndThreadPerf(message: String, daemonOptions: DaemonOptions, messageCollector: MessageCollector, profiler: Profiler) {
            if (daemonOptions.reportPerf) {
                fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
                val counters = profiler.getTotalCounters()
                messageCollector.report(CompilerMessageSeverity.INFO,
                                        "PERF: $message ${counters.time.ms()} ms, thread ${counters.threadTime.ms()}",
                                        CompilerMessageLocation.NO_LOCATION)
            }
        }

        reportTotalAndThreadPerf("Daemon connect", daemonOptions, environment.messageCollector, profiler)
        return connection
    }

    protected fun processCompilerOutput(
            environment: Env,
            stream: ByteArrayOutputStream,
            exitCode: ExitCode?
    ) {
        val reader = BufferedReader(StringReader(stream.toString()))
        CompilerOutputParser.parseCompilerMessagesFromReader(environment.messageCollector, reader, environment.outputItemsCollector)

        if (ExitCode.INTERNAL_ERROR == exitCode) {
            reportInternalCompilerError(environment.messageCollector)
        }
    }

    protected fun reportInternalCompilerError(messageCollector: MessageCollector): ExitCode {
        messageCollector.report(ERROR, "Compiler terminated with internal error", CompilerMessageLocation.NO_LOCATION)
        return ExitCode.INTERNAL_ERROR
    }

    protected fun runCompiler(
            compilerClassName: String,
            arguments: CommonCompilerArguments,
            additionalArguments: String,
            environment: Env): ExitCode {
        return try {
            val argumentsList = ArgumentUtils.convertArgumentsToStringList(arguments)
            argumentsList.addAll(additionalArguments.split(" "))

            val argsArray = argumentsList.toTypedArray()
            doRunCompiler(compilerClassName, argsArray, environment)
        }
        catch (e: Throwable) {
            MessageCollectorUtil.reportException(environment.messageCollector, e)
            reportInternalCompilerError(environment.messageCollector)
        }
    }

    protected abstract fun doRunCompiler(
            compilerClassName: String,
            argsArray: Array<String>,
            environment: Env
    ): ExitCode

    /**
     * Returns null if could not connect to daemon
     */
    protected open fun compileWithDaemon(
            compilerClassName: String,
            argsArray: Array<String>,
            environment: Env
    ): ExitCode? {
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

        val res: Int = withDaemon(environment, retryOnConnectionError = true) { daemon, sessionId ->
            KotlinCompilerClient.incrementalCompile(daemon, sessionId, targetPlatform, argsArray, services, compilerOut, daemonOut)
        } ?: return null

        val exitCode = exitCodeFromProcessExitCode(res)
        processCompilerOutput(environment, compilerOut, exitCode)
        BufferedReader(StringReader(daemonOut.toString())).forEachLine {
            environment.messageCollector.report(CompilerMessageSeverity.INFO, it, CompilerMessageLocation.NO_LOCATION)
        }
        return exitCode
    }

    protected fun <T> withDaemon(environment: Env, retryOnConnectionError: Boolean, fn: (CompileService, sessionId: Int)->T): T? {
        fun retryOrFalse(e: Exception): T? {
            if (retryOnConnectionError) {
                log.debug("retrying once on daemon connection error: ${e.message}")
                return withDaemon(environment, retryOnConnectionError = false, fn = fn)
            }
            log.info("daemon connection error: ${e.message}")
            return null
        }

        log.debug("Try to connect to daemon")
        val connection = getDaemonConnection(environment)

        if (connection.daemon != null) {
            log.info("Connected to daemon")

            try {
                return fn(connection.daemon, connection.sessionId)
            }
            catch (e: java.rmi.ConnectException) {
                return retryOrFalse(e)
            }
            catch (e: java.rmi.UnmarshalException) {
                return retryOrFalse(e)
            }
        }

        log.info("Daemon not found")
        return null
    }

    protected fun exitCodeFromProcessExitCode(code: Int): ExitCode {
        val exitCode = ExitCode.values().find { it.code == code }
        if (exitCode != null) return exitCode

        log.debug("Could not find exit code by value: $code")
        return if (code == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR
    }
}

