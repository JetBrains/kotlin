/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental.common

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.experimental.new.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.CompilationResultsServerSide
import org.jetbrains.kotlin.daemon.common.experimental.CompileServiceClientSide
import org.jetbrains.kotlin.daemon.common.experimental.DummyProfiler
import org.jetbrains.kotlin.daemon.common.experimental.Profiler
import java.io.File

interface KotlinCompilerDaemonClient {
    suspend fun connectToCompileService(
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean = true,
        checkId: Boolean = true
    ): CompileServiceClientSide?

    suspend fun connectToCompileService(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean = true
    ): CompileServiceClientSide?

    suspend fun connectAndLease(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean,
        leaseSession: Boolean,
        sessionAliveFlagFile: File? = null
    ): CompileServiceSession?

    suspend fun shutdownCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions)

    suspend fun leaseCompileSession(compilerService: CompileServiceClientSide, aliveFlagPath: String?): Int
    suspend fun releaseCompileSession(compilerService: CompileServiceClientSide, sessionId: Int): CompileService.CallResult<Unit>
    suspend fun compile(
        compilerService: CompileServiceClientSide,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        messageCollector: MessageCollector,
        outputsCollector: ((File, List<File>) -> Unit)? = null,
        compilerMode: CompilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
        reportSeverity: ReportSeverity = ReportSeverity.INFO,
        profiler: Profiler = DummyProfiler()
    ): Int

    fun createCompResults(): CompilationResultsServerSide

    enum class Version {
        RMI, SOCKETS
    }

    companion object {
        fun instantiate(version: Version): KotlinCompilerDaemonClient =
            when (version) {
                Version.RMI -> {
                    org.jetbrains.kotlin.daemon.client.experimental.new.KotlinCompilerClient
                }
                Version.SOCKETS -> {
                    org.jetbrains.kotlin.daemon.client.experimental.old.KotlinCompilerClient
                }
            }
    }

}