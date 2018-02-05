/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package org.jetbrains.kotlin.daemon.common.experimental

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*
import java.io.File

interface CompileServiceClientSide : CompileServiceAsync, Client

class CompileServiceClientSideImpl(
    host: String,
    port: Int,
    socketFactory: LoopbackNetworkInterface.ClientLoopbackSocketFactoryKtor
) : CompileServiceClientSide {

    lateinit var socketToServer: Socket
    val input: ByteReadChannelWrapper by lazy { socketToServer.openAndWrapReadChannel() }
    val output: ByteWriteChannelWrapper by lazy { socketToServer.openAndWrapWriteChannel() }

    // CompileService methods:

    override suspend fun checkCompilerId(expectedCompilerId: CompilerId): Boolean {
        output.writeObject(
            CheckCompilerIdMessage(
                expectedCompilerId
            )
        )
        return input.nextObject() as Boolean
    }

    override suspend fun getUsedMemory(): CompileService.CallResult<Long> {
        output.writeObject(GetUsedMemoryMessage())
        return input.nextObject() as CompileService.CallResult<Long>
    }


    override suspend fun getDaemonOptions(): CompileService.CallResult<DaemonOptions> {
        output.writeObject(GetDaemonOptionsMessage())
        return input.nextObject() as CompileService.CallResult<DaemonOptions>
    }

    override suspend fun getDaemonInfo(): CompileService.CallResult<String> {
        output.writeObject(GetDaemonInfoMessage())
        return input.nextObject() as CompileService.CallResult<String>
    }

    override suspend fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions> {
        output.writeObject(GetDaemonJVMOptionsMessage())
        return input.nextObject() as CompileService.CallResult<DaemonJVMOptions>
    }

    override suspend fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> {
        output.writeObject(RegisterClientMessage(aliveFlagPath))
        return input.nextObject() as CompileService.CallResult<Nothing>
    }

    override suspend fun getClients(): CompileService.CallResult<List<String>> {
        output.writeObject(GetClientsMessage())
        return input.nextObject() as CompileService.CallResult<List<String>>
    }

    override suspend fun leaseCompileSession(aliveFlagPath: String?): CompileService.CallResult<Int> {
        output.writeObject(
            LeaseCompileSessionMessage(
                aliveFlagPath
            )
        )
        return input.nextObject() as CompileService.CallResult<Int>
    }

    override suspend fun releaseCompileSession(sessionId: Int): CompileService.CallResult<Nothing> {
        output.writeObject(
            ReleaseCompileSessionMessage(
                sessionId
            )
        )
        return input.nextObject() as CompileService.CallResult<Nothing>
    }

    override suspend fun shutdown(): CompileService.CallResult<Nothing> {
        output.writeObject(ShutdownMessage())
        return input.nextObject() as CompileService.CallResult<Nothing>
    }

    override suspend fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> {
        output.writeObject(ScheduleShutdownMessage(graceful))
        return input.nextObject() as CompileService.CallResult<Boolean>
    }

    override suspend fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        compilationResults: CompilationResults?
    ): CompileService.CallResult<Int> {
        output.writeObject(
            CompileMessage(
                sessionId,
                compilerArguments,
                compilationOptions,
                servicesFacade,
                compilationResults
            )
        )
        return input.nextObject() as CompileService.CallResult<Int>
    }

    override suspend fun clearJarCache() {
        output.writeObject(ClearJarCacheMessage())
    }

    override suspend fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing> {
        output.writeObject(ReleaseReplSessionMessage(sessionId))
        return input.nextObject() as CompileService.CallResult<Nothing>
    }

    override suspend fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        templateClasspath: List<File>,
        templateClassName: String
    ): CompileService.CallResult<Int> {
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
        return input.nextObject() as CompileService.CallResult<Int>
    }

    override suspend fun replCreateState(sessionId: Int): CompileService.CallResult<ReplStateFacade> {
        output.writeObject(ReplCreateStateMessage(sessionId))
        return input.nextObject() as CompileService.CallResult<ReplStateFacade>
    }

    override suspend fun replCheck(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCheckResult> {
        output.writeObject(
            ReplCheckMessage(
                sessionId,
                replStateId,
                codeLine
            )
        )
        return input.nextObject() as CompileService.CallResult<ReplCheckResult>
    }

    override suspend fun replCompile(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCompileResult> {
        output.writeObject(
            ReplCompileMessage(
                sessionId,
                replStateId,
                codeLine
            )
        )
        return input.nextObject() as CompileService.CallResult<ReplCompileResult>
    }


    // Client methods:
    override fun attachToServer(socket: Socket) {
        socketToServer = socket
    }


    // Query messages:

    class CheckCompilerIdMessage(val expectedCompilerId: CompilerId) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.checkCompilerId(expectedCompilerId))
    }

    class GetUsedMemoryMessage : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getUsedMemory())
    }

    class GetDaemonOptionsMessage : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getDaemonOptions())
    }

    class GetDaemonJVMOptionsMessage : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getDaemonJVMOptions())
    }

    class GetDaemonInfoMessage : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getDaemonInfo())
    }

    class RegisterClientMessage(val aliveFlagPath: String?) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.registerClient(aliveFlagPath))
    }


    class GetClientsMessage : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getClients())
    }

    class LeaseCompileSessionMessage(val aliveFlagPath: String?) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.leaseCompileSession(aliveFlagPath))
    }

    class ReleaseCompileSessionMessage(val sessionId: Int) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.releaseCompileSession(sessionId))
    }

    class ShutdownMessage : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.shutdown())
    }

    class ScheduleShutdownMessage(val graceful: Boolean) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.scheduleShutdown(graceful))
    }

    class CompileMessage(
        val sessionId: Int,
        val compilerArguments: Array<out String>,
        val compilationOptions: CompilationOptions,
        val servicesFacade: CompilerServicesFacadeBase,
        val compilationResults: CompilationResults?
    ) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
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
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            server.clearJarCache()
    }

    class ReleaseReplSessionMessage(val sessionId: Int) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.releaseReplSession(sessionId))
    }

    class LeaseReplSession_Short_Message(
        val aliveFlagPath: String?,
        val compilerArguments: Array<out String>,
        val compilationOptions: CompilationOptions,
        val servicesFacade: CompilerServicesFacadeBase,
        val templateClasspath: List<File>,
        val templateClassName: String
    ) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
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
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.replCreateState(sessionId))
    }

    class ReplCheckMessage(
        val sessionId: Int,
        val replStateId: Int,
        val codeLine: ReplCodeLine
    ) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.replCheck(sessionId, replStateId, codeLine))
    }

    class ReplCompileMessage(
        val sessionId: Int,
        val replStateId: Int,
        val codeLine: ReplCodeLine
    ) : Server.Message<CompileServiceServerSide> {
        override suspend fun process(server: CompileServiceServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.replCompile(sessionId, replStateId, codeLine))
    }

    init {
        attachToServer(socketFactory.createSocket(host, port))
    }

}