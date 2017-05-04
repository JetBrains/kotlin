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
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorUtil
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.*
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

    val isDebugEnabled: Boolean
}

abstract class KotlinCompilerRunner<in Env : CompilerEnvironment> {
    protected val K2JVM_COMPILER = "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler"
    protected val K2JS_COMPILER = "org.jetbrains.kotlin.cli.js.K2JSCompiler"
    protected val K2METADATA_COMPILER = "org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler"
    protected val INTERNAL_ERROR = ExitCode.INTERNAL_ERROR.toString()

    protected abstract val log: KotlinLogger

    protected abstract fun getDaemonConnection(environment: Env): CompileServiceSession?

    @Synchronized
    protected fun newDaemonConnection(
            compilerId: CompilerId,
            clientAliveFlagFile: File,
            sessionAliveFlagFile: File,
            environment: Env,
            daemonOptions: DaemonOptions = configureDaemonOptions()
    ): CompileServiceSession? {
        val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = true, inheritOtherJvmOptions = false, inheritAdditionalProperties = true)

        val daemonReportMessages = ArrayList<DaemonReportMessage>()
        val daemonReportingTargets = DaemonReportingTargets(messages = daemonReportMessages)

        val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

        val connection = profiler.withMeasure(null) {
            KotlinCompilerClient.connectAndLease(compilerId,
                                                 clientAliveFlagFile,
                                                 daemonJVMOptions,
                                                 daemonOptions,
                                                 daemonReportingTargets,
                                                 autostart = true,
                                                 leaseSession = true,
                                                 sessionAliveFlagFile = sessionAliveFlagFile)
        }

        if (connection == null || log.isDebugEnabled) {
            for (message in daemonReportMessages) {
                val severity = when(message.category) {
                    DaemonReportCategory.DEBUG -> CompilerMessageSeverity.INFO
                    DaemonReportCategory.INFO -> CompilerMessageSeverity.INFO
                    DaemonReportCategory.EXCEPTION -> CompilerMessageSeverity.EXCEPTION
                }
                environment.messageCollector.report(severity, message.message)
            }
        }

        fun reportTotalAndThreadPerf(message: String, daemonOptions: DaemonOptions, messageCollector: MessageCollector, profiler: Profiler) {
            if (daemonOptions.reportPerf) {
                fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
                val counters = profiler.getTotalCounters()
                messageCollector.report(CompilerMessageSeverity.INFO, "PERF: $message ${counters.time.ms()} ms, thread ${counters.threadTime.ms()}")
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
        messageCollector.report(CompilerMessageSeverity.ERROR, "Compiler terminated with internal error")
        return ExitCode.INTERNAL_ERROR
    }

    protected fun runCompiler(
            compilerClassName: String,
            compilerArgs: CommonCompilerArguments,
            environment: Env): ExitCode {
        return try {
            compileWithDaemonOrFallback(compilerClassName, compilerArgs, environment)
        }
        catch (e: Throwable) {
            MessageCollectorUtil.reportException(environment.messageCollector, e)
            reportInternalCompilerError(environment.messageCollector)
        }
    }

    protected abstract fun compileWithDaemonOrFallback(
            compilerClassName: String,
            compilerArgs: CommonCompilerArguments,
            environment: Env
    ): ExitCode

    /**
     * Returns null if could not connect to daemon
     */
    protected abstract fun compileWithDaemon(
            compilerClassName: String,
            compilerArgs: CommonCompilerArguments,
            environment: Env
    ): ExitCode?

    protected fun exitCodeFromProcessExitCode(code: Int): ExitCode {
        val exitCode = ExitCode.values().find { it.code == code }
        if (exitCode != null) return exitCode

        log.debug("Could not find exit code by value: $code")
        return if (code == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR
    }
}

