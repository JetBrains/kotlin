/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.KotlinCompilerDaemonClient.Companion.instantiate
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.common.DummyProfiler
import org.jetbrains.kotlin.daemon.common.Profiler
import java.io.File

data class CompileServiceSessionAsync(val compileService: CompileServiceAsync, val sessionId: Int)

fun CompileServiceSession.toAsync() = CompileServiceSessionAsync(this.compileService.toClient(), this.sessionId)
fun CompileServiceSessionAsync.toRMI() = CompileServiceSession(this.compileService.toRMI(), this.sessionId)

interface KotlinCompilerDaemonClient {
    suspend fun connectToCompileService(
            compilerId: CompilerId,
            daemonJVMOptions: DaemonJVMOptions,
            daemonOptions: DaemonOptions,
            reportingTargets: DaemonReportingTargets,
            autostart: Boolean,
            checkId: Boolean
    ): CompileServiceAsync?

    suspend fun connectToCompileService(
            compilerId: CompilerId,
            clientAliveFlagFile: File,
            daemonJVMOptions: DaemonJVMOptions,
            daemonOptions: DaemonOptions,
            reportingTargets: DaemonReportingTargets,
            autostart: Boolean
    ): CompileServiceAsync?

    suspend fun connectAndLease(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean,
        leaseSession: Boolean,
        sessionAliveFlagFile: File? = null
    ): CompileServiceSessionAsync?

    suspend fun shutdownCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions)

    suspend fun leaseCompileSession(compilerService: CompileServiceAsync, aliveFlagPath: String?): Int
    suspend fun releaseCompileSession(compilerService: CompileServiceAsync, sessionId: Int): Unit
    suspend fun compile(
            compilerService: CompileServiceAsync,
            sessionId: Int,
            targetPlatform: CompileService.TargetPlatform,
            args: Array<out String>,
            messageCollector: MessageCollector,
            outputsCollector: ((File, List<File>) -> Unit)? = null,
            compilerMode: CompilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            reportSeverity: ReportSeverity = ReportSeverity.INFO,
            port: Int = SOCKET_ANY_FREE_PORT,
            profiler: Profiler = DummyProfiler()
    ): Int

    fun getOrCreateClientFlagFile(daemonOptions: DaemonOptions): File

    fun createCompResults(): CompilationResultsAsync

    fun main(vararg args: String)

    companion object {
        fun instantiate(daemonProtocolVariant: DaemonProtocolVariant): KotlinCompilerDaemonClient =
            KotlinCompilerDaemonClient::class.java
                .classLoader
                .loadClass(
                    when(daemonProtocolVariant) {
                        DaemonProtocolVariant.RMI -> "org.jetbrains.kotlin.daemon.client.impls.KotlinCompilerClientImpl"
                        DaemonProtocolVariant.SOCKETS -> "org.jetbrains.kotlin.daemon.client.experimental.KotlinCompilerClient"
                    }
                )
                .newInstance() as KotlinCompilerDaemonClient
    }

}

object KotlinCompilerClientInstance {

    const val RMI_FLAG = "-old"
    const val SOCKETS_FLAG = "-new_with_sockets"

    @JvmStatic
    fun main(vararg args: String) {
        val clientInstance: KotlinCompilerDaemonClient? = when (args.last()) {
            SOCKETS_FLAG ->
                instantiate(DaemonProtocolVariant.SOCKETS)
            else ->
                instantiate(DaemonProtocolVariant.RMI)
        }
        clientInstance?.main(*args.sliceArray(0..args.lastIndex))
    }

}