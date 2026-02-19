/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.CompileServiceSession
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

object KotlinCompilerRunnerUtils {
    fun exitCodeFromProcessExitCode(log: KotlinLogger, code: Int): ExitCode {
        val exitCode = ExitCode.entries.find { it.code == code }
        if (exitCode != null) return exitCode

        log.debug("Could not find exit code by value: $code")
        return if (code == 0) ExitCode.OK else ExitCode.COMPILATION_ERROR
    }

    @Synchronized
    @JvmStatic
    fun newDaemonConnection(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        sessionAliveFlagFile: File,
        messageCollector: MessageCollector,
        isDebugEnabled: Boolean,
        daemonOptions: DaemonOptions = configureDaemonOptions(),
        additionalJvmParams: Array<String> = arrayOf()
    ): CompileServiceSession? = newDaemonConnection(
        compilerId,
        clientAliveFlagFile,
        sessionAliveFlagFile,
        messageCollector,
        isDebugEnabled,
        daemonOptions,
        configureDaemonJVMOptions(
            *additionalJvmParams,
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = false,
            inheritAdditionalProperties = true
        )
    )

    @Synchronized
    @JvmStatic
    fun newDaemonConnection(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        sessionAliveFlagFile: File,
        messageCollector: MessageCollector,
        isDebugEnabled: Boolean,
        daemonOptions: DaemonOptions = configureDaemonOptions(),
        daemonJVMOptions: DaemonJVMOptions
    ): CompileServiceSession? {
        val daemonReportMessages = ArrayList<DaemonReportMessage>()
        val daemonReportingTargets = DaemonReportingTargets(messages = daemonReportMessages)

        val profiler = if (daemonOptions.reportPerf) WallAndThreadAndMemoryTotalProfiler(withGC = false) else DummyProfiler()

        val connection = profiler.withMeasure(null) {
            KotlinCompilerClient.connectAndLease(
                compilerId,
                clientAliveFlagFile,
                daemonJVMOptions,
                daemonOptions,
                daemonReportingTargets,
                autostart = true,
                leaseSession = true,
                sessionAliveFlagFile = sessionAliveFlagFile
            )
        }

        if (connection == null || isDebugEnabled) {
            for (message in daemonReportMessages) {
                val severity = when (message.category) {
                    DaemonReportCategory.DEBUG -> CompilerMessageSeverity.LOGGING
                    DaemonReportCategory.INFO -> CompilerMessageSeverity.INFO
                    DaemonReportCategory.EXCEPTION -> CompilerMessageSeverity.EXCEPTION
                }
                messageCollector.report(severity, message.message)
            }
        }

        fun reportTotalAndThreadPerf(
            message: String, daemonOptions: DaemonOptions, messageCollector: MessageCollector, profiler: Profiler
        ) {
            if (daemonOptions.reportPerf) {
                fun Long.ms() = TimeUnit.NANOSECONDS.toMillis(this)
                val counters = profiler.getTotalCounters()
                messageCollector.report(
                    CompilerMessageSeverity.INFO, "PERF: $message ${counters.time.ms()} ms, thread ${counters.threadTime.ms()}"
                )
            }
        }

        reportTotalAndThreadPerf("Daemon connect", daemonOptions, MessageCollector.NONE, profiler)
        return connection
    }
}

