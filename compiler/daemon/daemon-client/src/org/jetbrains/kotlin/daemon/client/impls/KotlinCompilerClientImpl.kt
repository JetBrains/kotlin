/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.impls

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.*
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.common.Profiler
import java.io.File

object KotlinCompilerClientImpl : KotlinCompilerDaemonClient {

    val oldClient = KotlinCompilerClient

    override suspend fun connectToCompileService(
            compilerId: CompilerId,
            daemonJVMOptions: DaemonJVMOptions,
            daemonOptions: DaemonOptions,
            reportingTargets: DaemonReportingTargets,
            autostart: Boolean,
            checkId: Boolean
    ): CompileServiceAsync? = oldClient.connectToCompileService(
            compilerId,
            daemonJVMOptions,
            daemonOptions,
            reportingTargets,
            autostart,
            checkId
    )?.toClient()

    override suspend fun connectToCompileService(
            compilerId: CompilerId,
            clientAliveFlagFile: File,
            daemonJVMOptions: DaemonJVMOptions,
            daemonOptions: DaemonOptions,
            reportingTargets: DaemonReportingTargets,
            autostart: Boolean
    ): CompileServiceAsync? = oldClient.connectToCompileService(
            compilerId,
            clientAliveFlagFile,
            daemonJVMOptions,
            daemonOptions,
            reportingTargets,
            autostart
    )?.toClient()

    override suspend fun connectAndLease(
            compilerId: CompilerId,
            clientAliveFlagFile: File,
            daemonJVMOptions: DaemonJVMOptions,
            daemonOptions: DaemonOptions,
            reportingTargets: DaemonReportingTargets,
            autostart: Boolean,
            leaseSession: Boolean,
            sessionAliveFlagFile: File?
    ): CompileServiceSessionAsync? = oldClient.connectAndLease(
            compilerId,
            clientAliveFlagFile,
            daemonJVMOptions,
            daemonOptions,
            reportingTargets,
            autostart,
            leaseSession,
            sessionAliveFlagFile
    )?.toAsync()

    override suspend fun shutdownCompileService(
            compilerId: CompilerId,
            daemonOptions: DaemonOptions
    ) = oldClient.shutdownCompileService(
            compilerId,
            daemonOptions
    )

    override suspend fun leaseCompileSession(
            compilerService: CompileServiceAsync,
            aliveFlagPath: String?
    ) = oldClient.leaseCompileSession(
            compilerService.toRMI(),
            aliveFlagPath
    )

    override suspend fun releaseCompileSession(
            compilerService: CompileServiceAsync,
            sessionId: Int
    ) = oldClient.releaseCompileSession(
            compilerService.toRMI(),
            sessionId
    )

    override suspend fun compile(
            compilerService: CompileServiceAsync,
            sessionId: Int,
            targetPlatform: CompileService.TargetPlatform,
            args: Array<out String>,
            messageCollector: MessageCollector,
            outputsCollector: ((File, List<File>) -> Unit)?,
            compilerMode: CompilerMode,
            reportSeverity: ReportSeverity,
            port: Int,
            profiler: Profiler
    ): Int = oldClient.compile(
            compilerService.toRMI(),
            sessionId,
            targetPlatform,
            args,
            messageCollector,
            outputsCollector,
            compilerMode,
            reportSeverity,
            port,
            profiler
    )

    override fun getOrCreateClientFlagFile(daemonOptions: DaemonOptions): File = oldClient.getOrCreateClientFlagFile(daemonOptions)

    override fun createCompResults() = TODO("not implemented")

    override fun main(vararg args: String) = oldClient.main(*args)

}