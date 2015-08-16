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

    public interface RemoteIncrementalCache : Remote {
        throws(RemoteException::class)
        public fun getObsoletePackageParts(): Collection<String>

        throws(RemoteException::class)
        public fun getPackageData(fqName: String): ByteArray?

        throws(RemoteException::class)
        public fun close()
    }

    throws(RemoteException::class)
    public fun getCompilerId(): CompilerId

    throws(RemoteException::class)
    public fun getUsedMemory(): Long

    throws(RemoteException::class)
    public fun shutdown()

    throws(RemoteException::class)
    public fun remoteCompile(args: Array<out String>, errStream: RemoteOutputStream, outputFormat: OutputFormat): Int

    throws(RemoteException::class)
    public fun remoteIncrementalCompile(
            args: Array<String>,
            caches: Map<String, RemoteIncrementalCache>,
            outputStream: RemoteOutputStream,
            outputFormat: OutputFormat): Int
}
