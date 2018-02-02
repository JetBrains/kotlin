/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNCHECKED_CAST", "EXPERIMENTAL_FEATURE_WARNING")

package org.jetbrains.kotlin.daemon.experimental

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.daemon.experimental.common.*
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.*
import java.io.File
import java.util.*

class CompileServiceClientSide(
    compiler: CompilerSelector,
    compilerId: CompilerId,
    daemonOptions: DaemonOptions,
    daemonJVMOptions: DaemonJVMOptions,
    port: Int,
    timer: Timer,
    onShutdown: () -> Unit
) : CompileService, Client {

    lateinit var socketToServer: Socket
    val input: ByteReadChannelWrapper by lazy { socketToServer.openAndWrapReadChannel() }
    val output: ByteWriteChannelWrapper by lazy { socketToServer.openAndWrapWriteChannel() }

    // CompileService methods:

    override fun checkCompilerId(expectedCompilerId: CompilerId): Boolean = runBlocking {
        output.writeObject(CheckCompilerIdMessage(expectedCompilerId))
        input.nextObject() as Boolean
    }

    override fun getUsedMemory(): CompileService.CallResult<Long> = runBlocking {
        output.writeObject(GetUsedMemoryMessage())
        input.nextObject() as CompileService.CallResult<Long>
    }


    override fun getDaemonOptions(): CompileService.CallResult<DaemonOptions> = runBlocking {
        output.writeObject(GetDaemonOptionsMessage())
        input.nextObject() as CompileService.CallResult<DaemonOptions>
    }

    override fun getDaemonInfo(): CompileService.CallResult<String> = runBlocking {
        output.writeObject(GetDaemonInfoMessage())
        input.nextObject() as CompileService.CallResult<String>
    }

    override fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions> = runBlocking {
        output.writeObject(GetDaemonJVMOptionsMessage())
        input.nextObject() as CompileService.CallResult<DaemonJVMOptions>
    }

    override fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> = runBlocking {
        output.writeObject(RegisterClientMessage(aliveFlagPath))
        input.nextObject() as CompileService.CallResult<Nothing>
    }

    override fun getClients(): CompileService.CallResult<List<String>> = runBlocking {
        output.writeObject(GetClientsMessage())
        input.nextObject() as CompileService.CallResult<List<String>>
    }

    override fun leaseCompileSession(aliveFlagPath: String?): CompileService.CallResult<Int> = runBlocking {
        output.writeObject(LeaseCompileSessionMessage(aliveFlagPath))
        input.nextObject() as CompileService.CallResult<Int>
    }

    override fun releaseCompileSession(sessionId: Int): CompileService.CallResult<Nothing> = runBlocking {
        output.writeObject(ReleaseCompileSessionMessage(sessionId))
        input.nextObject() as CompileService.CallResult<Nothing>
    }

    override fun shutdown(): CompileService.CallResult<Nothing> = runBlocking {
        output.writeObject(ShutdownMessage())
        input.nextObject() as CompileService.CallResult<Nothing>
    }

    override fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> = runBlocking {
        output.writeObject(ScheduleShutdownMessage(graceful))
        input.nextObject() as CompileService.CallResult<Boolean>
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
    ): CompileService.CallResult<Int> = runBlocking {
        output.writeObject(
            RemoteCompileMessage(
                sessionId,
                targetPlatform,
                args,
                servicesFacade,
                compilerOutputStream,
                outputFormat,
                serviceOutputStream,
                operationsTracer
            )
        )
        input.nextObject() as CompileService.CallResult<Int>
    }

    override fun remoteIncrementalCompile(
        sessionId: Int,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        servicesFacade: CompilerCallbackServicesFacade,
        compilerOutputStream: RemoteOutputStream,
        compilerOutputFormat: CompileService.OutputFormat,
        serviceOutputStream: RemoteOutputStream,
        operationsTracer: RemoteOperationsTracer?
    ): CompileService.CallResult<Int> = runBlocking {
        output.writeObject(
            RemoteIncrementalCompileMessage(
                sessionId,
                targetPlatform,
                args,
                servicesFacade,
                compilerOutputStream,
                compilerOutputFormat,
                serviceOutputStream,
                operationsTracer
            )
        )
        input.nextObject() as CompileService.CallResult<Int>
    }

    override fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        compilationResults: CompilationResults?
    ): CompileService.CallResult<Int> = runBlocking {
        output.writeObject(
            CompileMessage(
                sessionId,
                compilerArguments,
                compilationOptions,
                servicesFacade,
                compilationResults
            )
        )
        input.nextObject() as CompileService.CallResult<Int>
    }

    override fun clearJarCache() = runBlocking { output.writeObject(ClearJarCacheMessage()) }

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
    ): CompileService.CallResult<Int> = runBlocking {
        output.writeObject(
            LeaseReplSessionMessage(
                aliveFlagPath,
                targetPlatform,
                servicesFacade,
                templateClasspath,
                templateClassName,
                scriptArgs,
                scriptArgsTypes,
                compilerMessagesOutputStream,
                evalOutputStream,
                evalErrorStream,
                evalInputStream,
                operationsTracer
            )
        )
        input.nextObject() as CompileService.CallResult<Int>
    }

    override fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing> = runBlocking {
        output.writeObject(ReleaseReplSessionMessage(sessionId))
        input.nextObject() as CompileService.CallResult<Nothing>
    }

    override fun remoteReplLineCheck(sessionId: Int, codeLine: ReplCodeLine): CompileService.CallResult<ReplCheckResult> = runBlocking {
        output.writeObject(RemoteReplLineCheckMessage(sessionId, codeLine))
        input.nextObject() as CompileService.CallResult<ReplCheckResult>
    }

    override fun remoteReplLineCompile(
        sessionId: Int,
        codeLine: ReplCodeLine,
        history: List<ReplCodeLine>?
    ): CompileService.CallResult<ReplCompileResult> = runBlocking {
        output.writeObject(RemoteReplLineCompileMessage(sessionId, codeLine, history))
        input.nextObject() as CompileService.CallResult<ReplCompileResult>
    }

    override fun remoteReplLineEval(
        sessionId: Int,
        codeLine: ReplCodeLine,
        history: List<ReplCodeLine>?
    ): CompileService.CallResult<ReplEvalResult> = runBlocking {
        output.writeObject(RemoteReplLineEvalMessage(sessionId, codeLine, history))
        input.nextObject() as CompileService.CallResult<ReplEvalResult>
    }

    override fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        templateClasspath: List<File>,
        templateClassName: String
    ): CompileService.CallResult<Int> = runBlocking {
        output.writeObject(
            LeaseReplSession_Short_Message(
                aliveFlagPath,
                compilerArguments,
                compilationOptions,
                servicesFacade,
                templateClasspath,
                templateClassName
            )
        )
        input.nextObject() as CompileService.CallResult<Int>
    }

    override fun replCreateState(sessionId: Int): CompileService.CallResult<ReplStateFacade> = runBlocking {
        output.writeObject(ReplCreateStateMessage(sessionId))
        input.nextObject() as CompileService.CallResult<ReplStateFacade>
    }

    override fun replCheck(sessionId: Int, replStateId: Int, codeLine: ReplCodeLine): CompileService.CallResult<ReplCheckResult> =
        runBlocking {
            output.writeObject(ReplCheckMessage(sessionId, replStateId, codeLine))
            input.nextObject() as CompileService.CallResult<ReplCheckResult>
        }

    override fun replCompile(sessionId: Int, replStateId: Int, codeLine: ReplCodeLine): CompileService.CallResult<ReplCompileResult> =
        runBlocking {
            output.writeObject(ReplCompileMessage(sessionId, replStateId, codeLine))
            input.nextObject() as CompileService.CallResult<ReplCompileResult>
        }


    // Client methods:
    override fun attachToServer(socket: Socket) {
        socketToServer = socket
    }


    // Query messages:

    class CheckCompilerIdMessage(val expectedCompilerId: CompilerId) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.checkCompilerId(expectedCompilerId))
    }

    class GetUsedMemoryMessage : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getUsedMemory())
    }

    class GetDaemonOptionsMessage : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getDaemonOptions())
    }

    class GetDaemonJVMOptionsMessage : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getDaemonJVMOptions())
    }

    class GetDaemonInfoMessage : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getDaemonInfo())
    }

    class RegisterClientMessage(val aliveFlagPath: String?) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.registerClient(aliveFlagPath))
    }


    class GetClientsMessage : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getClients())
    }

    class LeaseCompileSessionMessage(val aliveFlagPath: String?) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.leaseCompileSession(aliveFlagPath))
    }

    class ReleaseCompileSessionMessage(val sessionId: Int) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.releaseCompileSession(sessionId))
    }

    class ShutdownMessage : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.shutdown())
    }

    class ScheduleShutdownMessage(val graceful: Boolean) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.scheduleShutdown(graceful))
    }


    class RemoteCompileMessage(
        val sessionId: Int,
        val targetPlatform: CompileService.TargetPlatform,
        val args: Array<out String>,
        val servicesFacade: CompilerCallbackServicesFacade,
        val compilerOutputStream: RemoteOutputStream,
        val outputFormat: CompileService.OutputFormat,
        val serviceOutputStream: RemoteOutputStream,
        val operationsTracer: RemoteOperationsTracer?
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(
                server.remoteCompile(
                    sessionId,
                    targetPlatform,
                    args,
                    servicesFacade,
                    compilerOutputStream,
                    outputFormat,
                    serviceOutputStream,
                    operationsTracer
                )
            )
    }


    class RemoteIncrementalCompileMessage(
        val sessionId: Int,
        val targetPlatform: CompileService.TargetPlatform,
        val args: Array<out String>,
        val servicesFacade: CompilerCallbackServicesFacade,
        val compilerOutputStream: RemoteOutputStream,
        val compilerOutputFormat: CompileService.OutputFormat,
        val serviceOutputStream: RemoteOutputStream,
        val operationsTracer: RemoteOperationsTracer?
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(
                server.remoteIncrementalCompile(
                    sessionId,
                    targetPlatform,
                    args,
                    servicesFacade,
                    compilerOutputStream,
                    compilerOutputFormat,
                    serviceOutputStream,
                    operationsTracer
                )
            )
    }

    class CompileMessage(
        val sessionId: Int,
        val compilerArguments: Array<out String>,
        val compilationOptions: CompilationOptions,
        val servicesFacade: CompilerServicesFacadeBase,
        val compilationResults: CompilationResults?
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(
                server.compile(
                    sessionId,
                    compilerArguments,
                    compilationOptions,
                    servicesFacade,
                    compilationResults
                )
            )
    }

    class ClearJarCacheMessage : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            server.clearJarCache()
    }

    class LeaseReplSessionMessage(
        val aliveFlagPath: String?,
        val targetPlatform: CompileService.TargetPlatform,
        val servicesFacade: CompilerCallbackServicesFacade,
        val templateClasspath: List<File>,
        val templateClassName: String,
        val scriptArgs: Array<out Any?>?,
        val scriptArgsTypes: Array<out Class<out Any>>?,
        val compilerMessagesOutputStream: RemoteOutputStream,
        val evalOutputStream: RemoteOutputStream?,
        val evalErrorStream: RemoteOutputStream?,
        val evalInputStream: RemoteInputStream?,
        val operationsTracer: RemoteOperationsTracer?
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(
                server.leaseReplSession(
                    aliveFlagPath,
                    targetPlatform,
                    servicesFacade,
                    templateClasspath,
                    templateClassName,
                    scriptArgs,
                    scriptArgsTypes,
                    compilerMessagesOutputStream,
                    evalOutputStream,
                    evalErrorStream,
                    evalInputStream,
                    operationsTracer
                )
            )
    }

    class ReleaseReplSessionMessage(val sessionId: Int) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.releaseReplSession(sessionId))
    }

    class RemoteReplLineCheckMessage(
        val sessionId: Int,
        val codeLine: ReplCodeLine
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.remoteReplLineCheck(sessionId, codeLine))
    }

    class RemoteReplLineCompileMessage(
        val sessionId: Int,
        val codeLine: ReplCodeLine,
        val history: List<ReplCodeLine>?
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.remoteReplLineCompile(sessionId, codeLine, history))
    }

    class RemoteReplLineEvalMessage(
        val sessionId: Int,
        val codeLine: ReplCodeLine,
        val history: List<ReplCodeLine>?
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.remoteReplLineEval(sessionId, codeLine, history))
    }

    class LeaseReplSession_Short_Message(
        val aliveFlagPath: String?,
        val compilerArguments: Array<out String>,
        val compilationOptions: CompilationOptions,
        val servicesFacade: CompilerServicesFacadeBase,
        val templateClasspath: List<File>,
        val templateClassName: String
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(
                server.leaseReplSession(
                    aliveFlagPath,
                    compilerArguments,
                    compilationOptions,
                    servicesFacade,
                    templateClasspath,
                    templateClassName
                )
            )
    }

    class ReplCreateStateMessage(val sessionId: Int) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.replCreateState(sessionId))
    }

    class ReplCheckMessage(
        val sessionId: Int,
        val replStateId: Int,
        val codeLine: ReplCodeLine
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.replCheck(sessionId, replStateId, codeLine))
    }

    class ReplCompileMessage(
        val sessionId: Int,
        val replStateId: Int,
        val codeLine: ReplCodeLine
    ) : Server.Message<CompileServiceServerSide> {
        suspend override fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.replCompile(sessionId, replStateId, codeLine))
    }

}