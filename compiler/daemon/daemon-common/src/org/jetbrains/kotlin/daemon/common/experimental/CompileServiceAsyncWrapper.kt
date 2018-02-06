/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import java.io.File

class CompileServiceRMIAsyncWrapper(val rmiCompileService: CompileService) : CompileServiceAsync {

    suspend override fun getUsedMemory() = runBlocking {
        rmiCompileService.getUsedMemory()
    }

    suspend override fun getDaemonOptions() = runBlocking {
        rmiCompileService.getDaemonOptions()
    }

    suspend override fun getDaemonInfo() = runBlocking {
        rmiCompileService.getDaemonInfo()
    }

    suspend override fun getDaemonJVMOptions() = runBlocking {
        rmiCompileService.getDaemonJVMOptions()
    }

    suspend override fun registerClient(aliveFlagPath: String?) = runBlocking {
        rmiCompileService.registerClient(aliveFlagPath)
    }

    suspend override fun getClients() = runBlocking {
        rmiCompileService.getClients()
    }

    suspend override fun leaseCompileSession(aliveFlagPath: String?) = runBlocking {
        rmiCompileService.leaseCompileSession(aliveFlagPath)
    }

    suspend override fun releaseCompileSession(sessionId: Int) = runBlocking {
        rmiCompileService.releaseCompileSession(sessionId)
    }

    suspend override fun shutdown() = runBlocking {
        rmiCompileService.shutdown()
    }

    suspend override fun scheduleShutdown(graceful: Boolean) = runBlocking {
        rmiCompileService.scheduleShutdown(graceful)
    }

    suspend override fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        compilationResults: CompilationResults?
    ) = runBlocking {
        rmiCompileService.compile(
            sessionId,
            compilerArguments,
            compilationOptions,
            (servicesFacade as CompilerServicesFacadeBaseAsyncWrapper).rmiImpl,
            compilationResults
        )
    }

    suspend override fun clearJarCache() = runBlocking {
        rmiCompileService.clearJarCache()
    }

    suspend override fun releaseReplSession(sessionId: Int) = runBlocking {
        rmiCompileService.releaseReplSession(sessionId)
    }

    suspend override fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        templateClasspath: List<File>,
        templateClassName: String
    ) = runBlocking {
        rmiCompileService.leaseReplSession(
            aliveFlagPath,
            compilerArguments,
            compilationOptions,
            (servicesFacade as CompilerServicesFacadeBaseAsyncWrapper).rmiImpl,
            templateClasspath,
            templateClassName
        )
    }

    suspend override fun replCreateState(sessionId: Int) = runBlocking {
        rmiCompileService.replCreateState(sessionId).toWrapper()
    }

    suspend override fun replCheck(sessionId: Int, replStateId: Int, codeLine: ReplCodeLine) = runBlocking {
        rmiCompileService.replCheck(sessionId, replStateId, codeLine)
    }

    suspend override fun replCompile(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ) = runBlocking {
        rmiCompileService.replCompile(sessionId, replStateId, codeLine)
    }

    suspend override fun checkCompilerId(expectedCompilerId: CompilerId)= runBlocking {
        rmiCompileService.checkCompilerId(expectedCompilerId)
    }

}

fun CompileService.toWrapper() = CompileServiceRMIAsyncWrapper(this)