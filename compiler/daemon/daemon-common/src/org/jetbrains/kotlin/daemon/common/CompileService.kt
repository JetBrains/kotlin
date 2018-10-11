/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.daemon.common.impls.*
import java.io.File
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException

interface CompileService : Remote {

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

    // TODO: consider adding another client alive checking mechanism, e.g. socket/socketPort

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
    fun classesFqNamesByFiles(
        sessionId: Int,
        sourceFiles: Set<File>
    ): CallResult<Set<String>>

    @Throws(RemoteException::class)
    fun clearJarCache()

    @Deprecated("The usages should be replaced with other `leaseReplSession` method", ReplaceWith("leaseReplSession"))
    @Throws(RemoteException::class)
    fun leaseReplSession(
        aliveFlagPath: String?,
        targetPlatform: TargetPlatform,
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
}
