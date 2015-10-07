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
import java.rmi.Remote
import java.rmi.RemoteException


/**
 * common facade for compiler services exposed from client process (e.g. JPS) to the compiler running on daemon
 * the reason for having common facade is attempt to reduce number of connections between client and daemon
 * Note: non-standard naming convention used to denote combining several entities in one facade - prefix <entityName>_ is used for every function belonging to the entity
 */
public interface CompilerCallbackServicesFacade : Remote {

    @Throws(RemoteException::class)
    public fun hasIncrementalCaches(): Boolean

    @Throws(RemoteException::class)
    public fun hasLookupTracker(): Boolean

    @Throws(RemoteException::class)
    public fun hasCompilationCanceledStatus(): Boolean

    // ----------------------------------------------------
    // IncrementalCache
    @Throws(RemoteException::class)
    public fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String>

    @Throws(RemoteException::class)
    public fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String>

    @Throws(RemoteException::class)
    public fun incrementalCache_getMultifileFacade(target: TargetId, partInternalName: String): String?

    @Throws(RemoteException::class)
    public fun incrementalCache_getPackagePartData(target: TargetId, fqName: String): JvmPackagePartProto?

    @Throws(RemoteException::class)
    public fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray?

    @Throws(RemoteException::class)
    public fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String)

    @Throws(RemoteException::class)
    fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String

    @Throws(RemoteException::class)
    public fun incrementalCache_close(target: TargetId)

    @Throws(RemoteException::class)
    public fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>?

    // ----------------------------------------------------
    // LookupTracker
    @Throws(RemoteException::class)
    fun lookupTracker_record(
            lookupContainingFile: String,
            lookupLine: Int?,
            lookupColumn: Int?,
            scopeFqName: String,
            scopeKind: ScopeKind,
            name: String
    )
    
    @Throws(RemoteException::class)
    fun lookupTracker_isDoNothing(): Boolean
    
    // ----------------------------------------------------
    // CompilationCanceledStatus
    @Throws(RemoteException::class)
    fun compilationCanceledStatus_checkCanceled(): Unit
}

