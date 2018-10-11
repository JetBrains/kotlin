/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.impls.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.impls.CompilationResults
import org.jetbrains.kotlin.daemon.common.impls.ReportSeverity
import org.jetbrains.kotlin.daemon.common.impls.SOCKET_ANY_FREE_PORT
import java.io.File
import java.io.Serializable

data class CompileServiceSession(val compileService: CompileServiceAsync, val sessionId: Int)

fun org.jetbrains.kotlin.daemon.client.impls.CompileServiceSession.toWrapper() =
    CompileServiceSession(this.compileService.toClient(), this.sessionId)

class KotlinCompilerClient : KotlinCompilerDaemonClient {

    private val oldKotlinCompilerClient = org.jetbrains.kotlin.daemon.client.impls.KotlinCompilerClientImpl

    override suspend fun connectToCompileService(
        compilerId: CompilerId,
        daemonJVMOptions: DaemonJVMOptions,
        daemonOptions: DaemonOptions,
        reportingTargets: DaemonReportingTargets,
        autostart: Boolean,
        checkId: Boolean
    ): CompileServiceAsync? = oldKotlinCompilerClient.connectToCompileService(
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
    ): CompileServiceAsync? = oldKotlinCompilerClient.connectToCompileService(
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

    override suspend fun leaseCompileSession(compilerService: CompileServiceAsync, aliveFlagPath: String?): Int =
        oldKotlinCompilerClient.leaseCompileSession(compilerService.toRMI(), aliveFlagPath)

    override suspend fun releaseCompileSession(
        compilerService: CompileServiceAsync,
        sessionId: Int
    ) = runBlocking {
        oldKotlinCompilerClient.releaseCompileSession(compilerService.toRMI(), sessionId)
        CompileService.CallResult.Ok() // TODO
    }

    fun Profiler.toRMI() = object : org.jetbrains.kotlin.daemon.common.impls.Profiler {

        override fun getCounters() = this@toRMI.getCounters()

        override fun getTotalCounters() = this@toRMI.getTotalCounters()

        override fun <R> withMeasure(obj: Any?, body: () -> R): R = runBlocking {
            this@toRMI.withMeasure(obj) {
                body()
            }
        }

    }

    override suspend fun compile(
        compilerService: CompileServiceAsync,
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

    }

    private fun CompilationResultsServSideCompatible.toServer() =
        object : CompilationResultsAsync {
            override val clientSide: CompilationResultsAsync
                get() = this

            override suspend fun add(compilationResultCategory: Int, value: Serializable) =
                this@toServer.add(compilationResultCategory, value)
        }

    override fun createCompResults(): CompilationResultsAsync {
        val oldCompResults = object : CompilationResultsServSideCompatible {

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

    override fun main(vararg args: String) = oldKotlinCompilerClient.main(*args)

}