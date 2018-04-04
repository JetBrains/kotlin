/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package org.jetbrains.kotlin.daemon.common.experimental

import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.CompileService.CallResult
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultAuthorizableClient
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import java.io.File
import java.util.logging.Logger

interface CompileServiceClientSide : CompileServiceAsync, Client<CompileServiceServerSide> {
    val serverPort: Int
}


class CompileServiceClientSideImpl(
    override val serverPort: Int,
    val serverHost: String,
    val serverFile: File
) : CompileServiceClientSide,
    Client<CompileServiceServerSide> by object : DefaultAuthorizableClient<CompileServiceServerSide>(serverPort, serverHost) {

        override fun authorizeOnServer(serverOutputChannel: ByteWriteChannelWrapper) {
            runBlocking {
                log.info("in authoriseOnServer(serverFile=$serverFile)")
                val signature = serverFile.inputStream().use(::readTokenKeyPairAndSign)
                sendSignature(serverOutputChannel, signature)
            }
        }

    } {

    val log = Logger.getLogger("CompileServiceClientSideImpl")

    override suspend fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseClientSide,
        compilationResults: CompilationResultsClientSide?
    ): CallResult<Int> {
        sendMessage(CompileMessage(sessionId, compilerArguments, compilationOptions, servicesFacade, compilationResults)).await()
        return readMessage<CallResult<Int>>().await()
    }

    override suspend fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseClientSide,
        templateClasspath: List<File>,
        templateClassName: String
    ): CallResult<Int> {
        sendMessage(
            LeaseReplSessionMessage(
                aliveFlagPath,
                compilerArguments,
                compilationOptions,
                servicesFacade,
                templateClasspath,
                templateClassName
            )
        ).await()
        return readMessage<CallResult<Int>>().await()
    }

    // CompileService methods:

    override suspend fun checkCompilerId(expectedCompilerId: CompilerId): Boolean {
        sendMessage(
            CheckCompilerIdMessage(
                expectedCompilerId
            )
        ).await()
        return readMessage<Boolean>().await()
    }

    override suspend fun getUsedMemory(): CallResult<Long> {
        sendMessage(GetUsedMemoryMessage()).await()
        return readMessage<CallResult<Long>>().await()
    }


    override suspend fun getDaemonOptions(): CallResult<DaemonOptions> {
        sendMessage(GetDaemonOptionsMessage()).await()
        return readMessage<CallResult<DaemonOptions>>().await()
    }

    override suspend fun getDaemonInfo(): CallResult<String> {
        sendMessage(GetDaemonInfoMessage()).await()
        return readMessage<CallResult<String>>().await()
    }

    override suspend fun getDaemonJVMOptions(): CallResult<DaemonJVMOptions> {
        log.info("sending message (GetDaemonJVMOptionsMessage) ... (deaemon port = $serverPort)")
        sendMessage(GetDaemonJVMOptionsMessage()).await()
        log.info("message is sent!")
        val resAsync = readMessage<CallResult<DaemonJVMOptions>>()
        log.info("reading message...")
        val res = resAsync.await()
        log.info("reply : $res")
        return res
    }

    override suspend fun registerClient(aliveFlagPath: String?): CallResult<Nothing> {
        sendMessage(RegisterClientMessage(aliveFlagPath)).await()
        return readMessage<CallResult<Nothing>>().await()
    }

    override suspend fun getClients(): CallResult<List<String>> {
        sendMessage(GetClientsMessage()).await()
        return readMessage<CallResult<List<String>>>().await()
    }

    override suspend fun leaseCompileSession(aliveFlagPath: String?): CallResult<Int> {
        sendMessage(
            LeaseCompileSessionMessage(
                aliveFlagPath
            )
        ).await()
        return readMessage<CallResult<Int>>().await()
    }

    override suspend fun releaseCompileSession(sessionId: Int): CallResult<Nothing> {
        sendMessage(
            ReleaseCompileSessionMessage(
                sessionId
            )
        ).await()
        return readMessage<CallResult<Nothing>>().await()
    }

    override suspend fun shutdown(): CallResult<Nothing> {
        sendMessage(ShutdownMessage()).await()
        return readMessage<CallResult<Nothing>>().await()
    }

    override suspend fun scheduleShutdown(graceful: Boolean): CallResult<Boolean> {
        sendMessage(ScheduleShutdownMessage(graceful)).await()
        return readMessage<CallResult<Boolean>>().await()
    }

    override suspend fun clearJarCache() {
        sendMessage(ClearJarCacheMessage())
    }

    override suspend fun releaseReplSession(sessionId: Int): CallResult<Nothing> {
        sendMessage(ReleaseReplSessionMessage(sessionId)).await()
        return readMessage<CallResult<Nothing>>().await()
    }

    override suspend fun replCreateState(sessionId: Int): CallResult<ReplStateFacadeClientSide> {
        sendMessage(ReplCreateStateMessage(sessionId)).await()
        return readMessage<CallResult<ReplStateFacadeClientSide>>().await()
    }

    override suspend fun replCheck(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CallResult<ReplCheckResult> {
        sendMessage(
            ReplCheckMessage(
                sessionId,
                replStateId,
                codeLine
            )
        ).await()
        return readMessage<CallResult<ReplCheckResult>>().await()
    }

    override suspend fun replCompile(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CallResult<ReplCompileResult> {
        sendMessage(
            ReplCompileMessage(
                sessionId,
                replStateId,
                codeLine
            )
        ).await()
        return readMessage<CallResult<ReplCompileResult>>().await()
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
        val servicesFacade: CompilerServicesFacadeBaseClientSide,
        val compilationResults: CompilationResultsClientSide?
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

    class LeaseReplSessionMessage(
        val aliveFlagPath: String?,
        val compilerArguments: Array<out String>,
        val compilationOptions: CompilationOptions,
        val servicesFacade: CompilerServicesFacadeBaseClientSide,
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
                    servicesFacade.toClient(),
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

}