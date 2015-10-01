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

import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException

public interface RemoteIncrementalCache : Remote {
    @Throws(RemoteException::class)
    public fun getObsoletePackageParts(): Collection<String>

    @Throws(RemoteException::class)
    public fun getObsoleteMultifileClassFacades(): Collection<String>

    @Throws(RemoteException::class)
    public fun getMultifileFacade(partInternalName: String): String?

    @Throws(RemoteException::class)
    public fun getPackagePartData(fqName: String): JvmPackagePartProto?

    @Throws(RemoteException::class)
    public fun getModuleMappingData(): ByteArray?

    @Throws(RemoteException::class)
    public fun registerInline(fromPath: String, jvmSignature: String, toPath: String)

    @Throws(RemoteException::class)
    fun getClassFilePath(internalClassName: String): String

    @Throws(RemoteException::class)
    public fun close()

    @Throws(RemoteException::class)
    public fun getMultifileFacadeParts(internalName: String): Collection<String>?
}

public interface RemoteLookupTracker : Remote {
    @Throws(RemoteException::class)
    fun record(
            lookupContainingFile: String,
            lookupLine: Int?,
            lookupColumn: Int?,
            scopeFqName: String,
            scopeKind: ScopeKind,
            name: String
    )
    @Throws(RemoteException::class)
    fun isDoNothing(): Boolean
}

public interface RemoteIncrementalCompilationComponents : Remote {
    @Throws(RemoteException::class)
    public fun getIncrementalCache(target: TargetId): RemoteIncrementalCache

    @Throws(RemoteException::class)
    public fun getLookupTracker(): RemoteLookupTracker
}

public interface RemoteCompilationCanceledStatus : Remote {
    @Throws(RemoteException::class)
    fun checkCanceled(): Unit
}

public data class RemoteCompilationServices(
        public val incrementalCompilationComponents: RemoteIncrementalCompilationComponents? = null,
        public val compilationCanceledStatus: RemoteCompilationCanceledStatus? = null
) : Serializable

