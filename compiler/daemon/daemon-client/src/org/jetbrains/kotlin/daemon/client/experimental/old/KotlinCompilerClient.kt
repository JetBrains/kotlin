/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental.old

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.experimental.common.KotlinCompilerDaemonClient
import org.jetbrains.kotlin.daemon.client.experimental.new.CompileServiceSession
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.Profiler
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import java.io.File
import java.io.Serializable

object KotlinCompilerClient : KotlinCompilerDaemonClient {

    private val oldKotlinCompilerClient = org.jetbrains.kotlin.daemon.client.KotlinCompilerClient

    override suspend fun connectToCompileService(
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean,
        checkId: Boolean
    ): CompileServiceClientSide? = oldKotlinCompilerClient.connectToCompileService(
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
    ): CompileServiceClientSide? = oldKotlinCompilerClient.connectToCompileService(
        compilerId,
        clientAliveFlagFile,
        daemonJVMOptions,
        daemonOptions,
        reportingTargets,
        autostart
    )?.toClient()

    private fun org.jetbrains.kotlin.daemon.client.CompileServiceSession.toWrapper() = CompileServiceSession(
        this.compileService.toClient(),
        this.sessionId
    )

    private fun CompileServiceSession.unwrap() = org.jetbrains.kotlin.daemon.client.CompileServiceSession(
        this.compileService.toRMI(),
        this.sessionId
    )

    override suspend fun connectAndLease(
        compilerId: CompilerId,
        clientAliveFlagFile: File,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean,
        leaseSession: Boolean,
        sessionAliveFlagFile: File?
    ): CompileServiceSession? = oldKotlinCompilerClient.connectAndLease(
        compilerId,
        clientAliveFlagFile,
        daemonJVMOptions,
        daemonOptions,
        reportingTargets,
        autostart,
        leaseSession,
        sessionAliveFlagFile
    )?.toWrapper()

    override suspend fun shutdownCompileService(compilerId: CompilerId, daemonOptions: DaemonOptions) =
        oldKotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

    override suspend fun leaseCompileSession(compilerService: CompileServiceClientSide, aliveFlagPath: String?): Int =
        oldKotlinCompilerClient.leaseCompileSession(compilerService.toRMI(), aliveFlagPath)

    override suspend fun releaseCompileSession(
        compilerService: CompileServiceClientSide,
        sessionId: Int
    ) = runBlocking {
        oldKotlinCompilerClient.releaseCompileSession(compilerService.toRMI(), sessionId)
        CompileService.CallResult.Ok() // TODO
    }

    fun Profiler.toRMI() = object : org.jetbrains.kotlin.daemon.common.Profiler {

        override fun getCounters() = this@toRMI.getCounters()

        override fun getTotalCounters() = this@toRMI.getTotalCounters()

        override fun <R> withMeasure(obj: Any?, body: () -> R): R = runBlocking {
            this@toRMI.withMeasure(obj) {
                body()
            }
        }

    }

    override suspend fun compile(
        compilerService: CompileServiceClientSide,
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        messageCollector: MessageCollector,
        outputsCollector: ((File, List<File>) -> Unit)?,
        compilerMode: CompilerMode,
        reportSeverity: ReportSeverity,
        profiler: Profiler
    ) = runBlocking {
        oldKotlinCompilerClient.compile(
            compilerService.toRMI(),
            sessionId,
            targetPlatform,
            args,
            messageCollector,
            outputsCollector,
            compilerMode,
            reportSeverity,
            SOCKET_ANY_FREE_PORT,
            profiler.toRMI()
        )
    }

    interface CompilationResultsServSideCompatible : CompilationResults {
        val clients: HashMap<Socket, Server.ClientInfo>
    }

    private fun CompilationResultsServSideCompatible.toServer() = object : CompilationResultsServerSide {

        override suspend fun add(compilationResultCategory: Int, value: Serializable) =
            this@toServer.add(compilationResultCategory, value)

        override val clientSide: CompilationResultsClientSide
            get() = this@toServer.toClient()
        override val serverSocketWithPort: ServerSocketWrapper
            get() = TODO("not implemented")
        override val clients: HashMap<Socket, Server.ClientInfo>
            get() = this@toServer.clients
    }

    override fun createCompResults(): CompilationResultsServerSide {
        val oldCompResults = object : CompilationResultsServSideCompatible {
            override val clients = hashMapOf<Socket, Server.ClientInfo>()

            private val resultsPort = findPortForSocket(
                COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
                RESULTS_SERVER_PORTS_RANGE_START,
                RESULTS_SERVER_PORTS_RANGE_END
            )

            private val resultsMap = hashMapOf<Int, MutableList<Serializable>>()

            override fun add(compilationResultCategory: Int, value: Serializable) {
                synchronized(this) {
                    resultsMap.putIfAbsent(compilationResultCategory, mutableListOf())
                    resultsMap[compilationResultCategory]!!.add(value)
                    // TODO logger?
                }
            }

        }
        return oldCompResults.toServer()
    }

}