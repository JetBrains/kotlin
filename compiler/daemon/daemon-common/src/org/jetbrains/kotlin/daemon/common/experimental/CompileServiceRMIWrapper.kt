/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import java.io.File
import java.rmi.NoSuchObjectException
import java.rmi.server.UnicastRemoteObject
import java.util.*

class CompileServiceRMIWrapper(val server: CompileServiceServerSide, daemonOptions: DaemonOptions, compilerId: CompilerId) : CompileService {

    private fun deprecated(): Nothing = TODO("NEVER USE DEPRECATED METHODS, PLEASE!") // prints this todo message

    override fun checkCompilerId(expectedCompilerId: CompilerId) = runBlocking(Unconfined) {
        server.checkCompilerId(expectedCompilerId)
    }

    override fun getUsedMemory() = runBlocking(Unconfined) {
        server.getUsedMemory()
    }

    override fun getDaemonOptions() = runBlocking(Unconfined) {
        server.getDaemonOptions()
    }

    override fun getDaemonInfo() = runBlocking(Unconfined) {
        server.getDaemonInfo()
    }

    override fun getDaemonJVMOptions() = runBlocking(Unconfined) {
        server.getDaemonJVMOptions()
    }

    override fun registerClient(aliveFlagPath: String?) = runBlocking(Unconfined) {
        server.registerClient(aliveFlagPath)
    }

    override fun getClients() = runBlocking(Unconfined) {
        server.getClients()
    }

    override fun leaseCompileSession(aliveFlagPath: String?) = runBlocking(Unconfined) {
        server.leaseCompileSession(aliveFlagPath)
    }

    override fun releaseCompileSession(sessionId: Int) = runBlocking(Unconfined) {
        server.releaseCompileSession(sessionId)
    }

    override fun shutdown() = runBlocking(Unconfined) {
        server.shutdown()
    }

    override fun scheduleShutdown(graceful: Boolean) = runBlocking(Unconfined) {
        server.scheduleShutdown(graceful)
    }

    override fun remoteCompile(
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        servicesFacade: CompilerCallbackServicesFacade,
        compilerOutputStream: RemoteOutputStream,
        outputFormat: CompileService.OutputFormat,
        serviceOutputStream: RemoteOutputStream,
        operationsTracer: RemoteOperationsTracer?
    ) = deprecated()

    override fun remoteIncrementalCompile(
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        servicesFacade: CompilerCallbackServicesFacade,
        compilerOutputStream: RemoteOutputStream,
        compilerOutputFormat: CompileService.OutputFormat,
        serviceOutputStream: RemoteOutputStream,
        operationsTracer: RemoteOperationsTracer?
    ) = deprecated()

    override fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        compilationResults: CompilationResults?
    ) = runBlocking(Unconfined) {
        server.compile(
            sessionId,
            compilerArguments,
            compilationOptions,
            servicesFacade.toClient(),
            compilationResults?.toClient()
        )
    }

    override fun clearJarCache() = runBlocking(Unconfined) {
        server.clearJarCache()
    }

    override fun leaseReplSession(
        aliveFlagPath: String?,
        targetPlatform: CompileService.TargetPlatform,
        servicesFacade: CompilerCallbackServicesFacade,
        templateClasspath: List<File>,
        templateClassName: String,
        scriptArgs: Array<out Any?>?,
        scriptArgsTypes: Array<out Class<out Any>>?,
        compilerMessagesOutputStream: RemoteOutputStream,
        evalOutputStream: RemoteOutputStream?,
        evalErrorStream: RemoteOutputStream?,
        evalInputStream: RemoteInputStream?,
        operationsTracer: RemoteOperationsTracer?
    ) = deprecated()

    override fun releaseReplSession(sessionId: Int) = runBlocking(Unconfined) {
        server.releaseReplSession(sessionId)
    }

    override fun remoteReplLineCheck(sessionId: Int, codeLine: ReplCodeLine) = deprecated()

    override fun remoteReplLineCompile(
        sessionId: Int,
        codeLine: ReplCodeLine,
        history: List<ReplCodeLine>?
    ) = deprecated()

    override fun remoteReplLineEval(
        sessionId: Int,
        codeLine: ReplCodeLine,
        history: List<ReplCodeLine>?
    ) = deprecated()

    override fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        templateClasspath: List<File>,
        templateClassName: String
    ) = runBlocking(Unconfined) {
        server.leaseReplSession(
            aliveFlagPath,
            compilerArguments,
            compilationOptions,
            servicesFacade.toClient(),
            templateClasspath,
            templateClassName
        )
    }

    override fun replCreateState(sessionId: Int) = runBlocking(Unconfined) {
        server.replCreateState(sessionId).toRMI()
    }

    override fun replCheck(sessionId: Int, replStateId: Int, codeLine: ReplCodeLine) = runBlocking(Unconfined) {
        server.replCheck(sessionId, replStateId, codeLine)
    }

    override fun replCompile(sessionId: Int, replStateId: Int, codeLine: ReplCodeLine) = runBlocking(Unconfined) {
        server.replCompile(sessionId, replStateId, codeLine)
    }

    init {
        // assuming logically synchronized
        try {
            // cleanup for the case of incorrect restart and many other situations
            UnicastRemoteObject.unexportObject(this, false)
        } catch (e: NoSuchObjectException) {
            // ignoring if object already exported
        }

        val (registry, port) = findPortAndCreateRegistry(
            COMPILE_DAEMON_FIND_PORT_ATTEMPTS,
            RMI_WRAPPER_PORTS_RANGE_START,
            RMI_WRAPPER_PORTS_RANGE_END
        )

        val stub = UnicastRemoteObject.exportObject(
            this,
            port,
            LoopbackNetworkInterface.clientLoopbackSocketFactory,
            LoopbackNetworkInterface.serverLoopbackSocketFactory
        ) as CompileService

        registry.rebind(COMPILER_SERVICE_RMI_NAME, stub)

        // create file :
        val runFileDir = File(daemonOptions.runFilesPathOrDefault)
        runFileDir.mkdirs()
        val runFile = File(
            runFileDir,
            makeRunFilenameString(
                timestamp = "%tFT%<tH-%<tM-%<tS.%<tLZ".format(Calendar.getInstance(TimeZone.getTimeZone("Z"))),
                digest = compilerId.compilerClasspath.map { File(it).absolutePath }.distinctStringsDigest().toHexString(),
                port = port.toString()
            )
        )
        try {
            if (!runFile.createNewFile()) throw Exception("createNewFile returned false")
        } catch (e: Throwable) {
            throw IllegalStateException("Unable to create runServer file '${runFile.absolutePath}'", e)
        }
        runFile.deleteOnExit()
    }

}

fun CompileServiceServerSide.toRMIServer(daemonOptions: DaemonOptions, compilerId: CompilerId) =
    CompileServiceRMIWrapper(this, daemonOptions, compilerId)