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

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
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
        const val NO_SESSION: Int = 0
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

        class Error(val message: String?, val cause: Throwable?) : CallResult<Nothing>() {
            constructor(cause: Throwable) : this(message = null, cause = cause)
            constructor(message: String) : this(message = message, cause = null)

            override fun get(): Nothing = throw Exception(message, cause)
            override fun equals(other: Any?): Boolean = other is Error && this.message == other.message && this.cause == other.cause
            override fun hashCode(): Int = this::class.java.hashCode() + (cause?.hashCode() ?: 1) + (message?.hashCode() ?: 2) // see comment to Ok.hashCode
        }

        val isGood: Boolean get() = this is Good<*>

        abstract fun get(): R
    }

    // TODO: remove!
    @Throws(RemoteException::class)
    fun checkCompilerId(expectedCompilerId: CompilerId): Boolean

    //Call with [withGC=true] can cause performance issue
    @Throws(RemoteException::class)
    fun getUsedMemory(withGC: Boolean = true): CallResult<Long>

    @Throws(RemoteException::class)
    fun getDaemonOptions(): CallResult<DaemonOptions>

    @Throws(RemoteException::class)
    fun getDaemonInfo(): CallResult<String>

    @Throws(RemoteException::class)
    fun getKotlinVersion(): CallResult<String>

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

    @Throws(RemoteException::class)
    fun releaseReplSession(sessionId: Int): CallResult<Nothing>

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
