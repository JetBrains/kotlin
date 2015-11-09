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

package org.jetbrains.kotlin.rmi

import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException

public interface CompileService : Remote {

    public enum class OutputFormat : Serializable {
        PLAIN,
        XML
    }

    public enum class TargetPlatform : Serializable {
        JVM,
        JS
    }

    companion object {
        public val NO_SESSION: Int = 0
    }

    public sealed class CallResult<R> : Serializable {
        class Good<R>(val result: R) : CallResult<R>()
        class Ok : CallResult<Nothing>()
        class Dying : CallResult<Nothing>()
        class Error(val message: String) : CallResult<Nothing>()
        fun get(): R = when (this) {
            is Good<R> -> this.result
            is Dying -> throw IllegalStateException("Service is dying")
            is Error -> throw IllegalStateException(this.message)
            else -> throw IllegalStateException("Unknown state")
        }
    }

    // TODO: remove!
    @Throws(RemoteException::class)
    public fun checkCompilerId(expectedCompilerId: CompilerId): Boolean

    @Throws(RemoteException::class)
    public fun getUsedMemory(): CallResult<Long>

    @Throws(RemoteException::class)
    public fun getDaemonOptions(): CallResult<DaemonOptions>

    @Throws(RemoteException::class)
    public fun getDaemonJVMOptions(): CallResult<DaemonJVMOptions>

    @Throws(RemoteException::class)
    public fun registerClient(aliveFlagPath: String?): CallResult<Nothing>

    // TODO: consider adding another client alive checking mechanism, e.g. socket/port

    @Throws(RemoteException::class)
    public fun leaseCompileSession(aliveFlagPath: String?): CallResult<Int>

    @Throws(RemoteException::class)
    public fun releaseCompileSession(sessionId: Int): Unit

    @Throws(RemoteException::class)
    public fun shutdown(): Unit

    // TODO: consider adding async version of shutdown and release

    @Throws(RemoteException::class)
    public fun remoteCompile(
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
    public fun remoteIncrementalCompile(
            sessionId: Int,
            targetPlatform: TargetPlatform,
            args: Array<out String>,
            servicesFacade: CompilerCallbackServicesFacade,
            compilerOutputStream: RemoteOutputStream,
            compilerOutputFormat: OutputFormat,
            serviceOutputStream: RemoteOutputStream,
            operationsTracer: RemoteOperationsTracer?
    ): CallResult<Int>
}
