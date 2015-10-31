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

import java.rmi.Remote
import java.rmi.RemoteException

public interface CompileService : Remote {

    public enum class OutputFormat : java.io.Serializable {
        PLAIN,
        XML
    }

    public enum class TargetPlatform : java.io.Serializable {
        JVM,
        JS
    }

    @Throws(RemoteException::class)
    public fun getCompilerId(): CompilerId

    @Throws(RemoteException::class)
    public fun getUsedMemory(): Long

    @Throws(RemoteException::class)
    public fun shutdown()

    @Throws(RemoteException::class)
    public fun remoteCompile(
            targetPlatform: TargetPlatform,
            args: Array<out String>,
            servicesFacade: CompilerCallbackServicesFacade,
            compilerOutputStream: RemoteOutputStream,
            outputFormat: OutputFormat,
            serviceOutputStream: RemoteOutputStream,
            operationsTracer: RemoteOperationsTracer?
    ): Int

    @Throws(RemoteException::class)
    public fun remoteIncrementalCompile(
            targetPlatform: TargetPlatform,
            args: Array<out String>,
            servicesFacade: CompilerCallbackServicesFacade,
            compilerOutputStream: RemoteOutputStream,
            compilerOutputFormat: OutputFormat,
            serviceOutputStream: RemoteOutputStream,
            operationsTracer: RemoteOperationsTracer?
    ): Int
}
