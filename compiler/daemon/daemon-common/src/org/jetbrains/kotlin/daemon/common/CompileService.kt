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

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
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
        JS
    }

    enum class CompilerMode : Serializable {
        NON_INCREMENTAL_COMPILER,
        INCREMENTAL_COMPILER
    }

    companion object {
        val NO_SESSION: Int = 0
    }

    sealed class CallResult<out R> : Serializable {

        class Good<out R>(val result: R) : CallResult<R>() {
            override fun get(): R = result
            override fun equals(other: Any?): Boolean = other is Good<*> && this.result == other.result
            override fun hashCode(): Int = this.javaClass.hashCode() + (result?.hashCode() ?: 1)
        }
        class Ok : CallResult<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Gey is inapplicable to Ok call result")
            override fun equals(other: Any?): Boolean = other is Ok
            override fun hashCode(): Int = this.javaClass.hashCode() + 1 // avoiding clash with the hash of class itself
        }
        class Dying : CallResult<Nothing>() {
            override fun get(): Nothing = throw IllegalStateException("Service is dying")
            override fun equals(other: Any?): Boolean = other is Dying
            override fun hashCode(): Int = this.javaClass.hashCode() + 1 // see comment to Ok.hashCode
        }
        class Error(val message: String) : CallResult<Nothing>() {
            override fun get(): Nothing = throw Exception(message)
            override fun equals(other: Any?): Boolean = other is Error && this.message == other.message
            override fun hashCode(): Int = this.javaClass.hashCode() + message.hashCode()
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
            compilerMode: CompilerMode,
            targetPlatform: TargetPlatform,
            compilerArguments: CommonCompilerArguments,
            additionalCompilerArguments: AdditionalCompilerArguments,
            servicesFacade: CompilerServicesFacadeBase,
            operationsTracer: RemoteOperationsTracer?
    ): CallResult<Int>

    @Throws(RemoteException::class)
    fun clearJarCache()
}
