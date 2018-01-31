/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.experimental.common

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server.Message
import java.io.File
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException

interface CompileService : Remote, Server {

    enum class OutputFormat : Serializable {
        PLAIN,
        XML
    }

    enum class TargetPlatform : Serializable {
        JVM,
        JS,
        METADATA
    }

    companion object {
        val NO_SESSION: Int = 0
    }

    sealed class CallResult<out R> : Serializable {

        class Good<out R>(val result: R) : CallResult<R>() {
            override fun get(): R = result
            override fun equals(other: Any?): Boolean = other is Good<*> && this.result == other.result
            override fun hashCode(): Int = this::class.java.hashCode() + (result?.hashCode() ?: 1)
        }

        class Ok : CallResult<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Get is inapplicable to Ok call result")
            override fun equals(other: Any?): Boolean = other is Ok
            override fun hashCode(): Int = this::class.java.hashCode() + 1 // avoiding clash with the hash of class itself
        }

        class Dying : CallResult<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Service is dying")
            override fun equals(other: Any?): Boolean = other is Dying
            override fun hashCode(): Int = this::class.java.hashCode() + 1 // see comment to Ok.hashCode
        }

        class Error(val message: String) : CallResult<Nothing>() {
            override fun get(): Nothing = throw Exception(message)
            override fun equals(other: Any?): Boolean = other is Error && this.message == other.message
            override fun hashCode(): Int = this::class.java.hashCode() + message.hashCode()
        }

        val isGood: Boolean get() = this is Good<*>

        abstract fun get(): R
    }

    // TODO: remove!
    @Throws(RemoteException::class)
    fun checkCompilerId(expectedCompilerId: CompilerId): Boolean

    @Throws(RemoteException::class)
    fun getUsedMemory(): CallResult<Long>

    @Throws(RemoteException::class)
    fun getDaemonOptions(): CallResult<DaemonOptions>

    @Throws(RemoteException::class)
    fun getDaemonInfo(): CallResult<String>

    @Throws(RemoteException::class)
    fun getDaemonJVMOptions(): CallResult<DaemonJVMOptions>

    @Throws(RemoteException::class)
    fun registerClient(aliveFlagPath: String?): CallResult<Nothing>

    // TODO: consider adding another client alive checking mechanism, e.g. socket/port

    @Throws(RemoteException::class)
    fun getClients(): CallResult<List<String>>

    @Throws(RemoteException::class)
    fun leaseCompileSession(aliveFlagPath: String?): CallResult<Int>

    @Throws(RemoteException::class)
    fun releaseCompileSession(sessionId: Int): CallResult<Nothing>

    @Throws(RemoteException::class)
    fun shutdown(): CallResult<Nothing>

    @Throws(RemoteException::class)
    fun scheduleShutdown(graceful: Boolean): CallResult<Boolean>

    // TODO: consider adding async version of shutdown and release

    @Deprecated("The usages should be replaced with `compile` method", ReplaceWith("compile"))
    @Throws(RemoteException::class)
    fun remoteCompile(
            sessionId: Int,
            targetPlatform: TargetPlatform,
            args: Array<out String>,
            servicesFacade: CompilerCallbackServicesFacade,
            compilerOutputStream: RemoteOutputStream,
            outputFormat: OutputFormat,
            serviceOutputStream: RemoteOutputStream,
            operationsTracer: RemoteOperationsTracer?
    ): CallResult<Int>

    @Deprecated("The usages should be replaced with `compile` method", ReplaceWith("compile"))
    @Throws(RemoteException::class)
    fun remoteIncrementalCompile(
            sessionId: Int,
            targetPlatform: TargetPlatform,
            args: Array<out String>,
            servicesFacade: CompilerCallbackServicesFacade,
            compilerOutputStream: RemoteOutputStream,
            compilerOutputFormat: OutputFormat,
            serviceOutputStream: RemoteOutputStream,
            operationsTracer: RemoteOperationsTracer?
    ): CallResult<Int>

    @Throws(RemoteException::class)
    fun compile(
            sessionId: Int,
            compilerArguments: Array<out String>,
            compilationOptions: CompilationOptions,
            servicesFacade: CompilerServicesFacadeBase,
            compilationResults: CompilationResults?
    ): CallResult<Int>

    @Throws(RemoteException::class)
    fun clearJarCache()

    @Deprecated("The usages should be replaced with other `leaseReplSession` method", ReplaceWith("leaseReplSession"))
    @Throws(RemoteException::class)
    fun leaseReplSession(
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
    ): CallResult<Int>

    @Throws(RemoteException::class)
    fun releaseReplSession(sessionId: Int): CallResult<Nothing>

    @Deprecated("The usages should be replaced with `replCheck` method", ReplaceWith("replCheck"))
    @Throws(RemoteException::class)
    fun remoteReplLineCheck(
            sessionId: Int,
            codeLine: ReplCodeLine
    ): CallResult<ReplCheckResult>

    @Deprecated("The usages should be replaced with `replCompile` method", ReplaceWith("replCompile"))
    @Throws(RemoteException::class)
    fun remoteReplLineCompile(
            sessionId: Int,
            codeLine: ReplCodeLine,
            history: List<ReplCodeLine>?
    ): CallResult<ReplCompileResult>

    @Deprecated("Evaluation on daemon is not supported")
    @Throws(RemoteException::class)
    fun remoteReplLineEval(
            sessionId: Int,
            codeLine: ReplCodeLine,
            history: List<ReplCodeLine>?
    ): CallResult<ReplEvalResult>

    @Throws(RemoteException::class)
    fun leaseReplSession(
            aliveFlagPath: String?,
            compilerArguments: Array<out String>,
            compilationOptions: CompilationOptions,
            servicesFacade: CompilerServicesFacadeBase,
            templateClasspath: List<File>,
            templateClassName: String
    ): CallResult<Int>

    @Throws(RemoteException::class)
    fun replCreateState(sessionId: Int): CallResult<ReplStateFacade>

    @Throws(RemoteException::class)
    fun replCheck(
            sessionId: Int,
            replStateId: Int,
            codeLine: ReplCodeLine
    ): CallResult<ReplCheckResult>

    @Throws(RemoteException::class)
    fun replCompile(
            sessionId: Int,
            replStateId: Int,
            codeLine: ReplCodeLine
    ): CallResult<ReplCompileResult>


    // Query messages:

    class CheckCompilerIdMessage(val expectedCompilerId: CompilerId) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.checkCompilerId(expectedCompilerId))
    }

    class GetUsedMemoryMessage : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.getUsedMemory())
    }

    class GetDaemonOptionsMessage : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.getDaemonOptions())
    }

    class GetDaemonJVMOptionsMessage : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.getDaemonJVMOptions())
    }

    class GetDaemonInfoMessage : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.getDaemonInfo())
    }

    class RegisterClientMessage(val aliveFlagPath: String?) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.registerClient(aliveFlagPath))
    }


    class GetClientMessage : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.getClients())
    }

    class LeaseCompileSessionMessage(val aliveFlagPath: String?) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.leaseCompileSession(aliveFlagPath))
    }

    class ReleaseCompileSessionMessage(val sessionId: Int) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.releaseCompileSession(sessionId))
    }

    class ShutdownMessage : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.shutdown())
    }

    class ScheduleShutdownMessage(val graceful: Boolean) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.scheduleShutdown(graceful))
    }


    class RemoteCompileMessage(
            val sessionId: Int,
            val targetPlatform: TargetPlatform,
            val args: Array<out String>,
            val servicesFacade: CompilerCallbackServicesFacade,
            val compilerOutputStream: RemoteOutputStream,
            val outputFormat: OutputFormat,
            val serviceOutputStream: RemoteOutputStream,
            val operationsTracer: RemoteOperationsTracer?
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(
                        clientSocket,
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
            val targetPlatform: TargetPlatform,
            val args: Array<out String>,
            val servicesFacade: CompilerCallbackServicesFacade,
            val compilerOutputStream: RemoteOutputStream,
            val compilerOutputFormat: OutputFormat,
            val serviceOutputStream: RemoteOutputStream,
            val operationsTracer: RemoteOperationsTracer?
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(
                        clientSocket,
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
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(
                        clientSocket,
                        server.compile(
                                sessionId,
                                compilerArguments,
                                compilationOptions,
                                servicesFacade,
                                compilationResults
                        )
                )
    }

    class ClearJarCacheMessage : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
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
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(
                        clientSocket,
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

    class ReleaseReplSessionMessage(val sessionId: Int) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.releaseReplSession(sessionId))
    }

    class RemoteReplLineCheckMessage(
            val sessionId: Int,
            val codeLine: ReplCodeLine
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.remoteReplLineCheck(sessionId, codeLine))
    }

    class RemoteReplLineCompileMessage(
            val sessionId: Int,
            val codeLine: ReplCodeLine,
            val history: List<ReplCodeLine>?
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.remoteReplLineCompile(sessionId, codeLine, history))
    }

    class RemoteReplLineEvalMessage(
            val sessionId: Int,
            val codeLine: ReplCodeLine,
            val history: List<ReplCodeLine>?
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.remoteReplLineEval(sessionId, codeLine, history))
    }

    class LeaseReplSession_Short_Message(
            val aliveFlagPath: String?,
            val compilerArguments: Array<out String>,
            val compilationOptions: CompilationOptions,
            val servicesFacade: CompilerServicesFacadeBase,
            val templateClasspath: List<File>,
            val templateClassName: String
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.leaseReplSession(
                        aliveFlagPath,
                        compilerArguments,
                        compilationOptions,
                        servicesFacade,
                        templateClasspath,
                        templateClassName
                ))
    }

    class ReplCreateStateMessage(val sessionId: Int) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.replCreateState(sessionId))
    }

    class ReplCheckMessage(
            val sessionId: Int,
            val replStateId: Int,
            val codeLine: ReplCodeLine
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.replCheck(sessionId, replStateId, codeLine))
    }

    class ReplCompileMessage(
            val sessionId: Int,
            val replStateId: Int,
            val codeLine: ReplCodeLine
    ) : Message<CompileService> {
        suspend override fun process(server: CompileService, clientSocket: Socket) =
                server.send(clientSocket, server.replCompile(sessionId, replStateId, codeLine))
    }


}
