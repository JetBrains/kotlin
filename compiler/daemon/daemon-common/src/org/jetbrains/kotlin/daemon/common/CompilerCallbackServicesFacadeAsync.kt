/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId

interface CompilerCallbackServicesFacadeAsync : CompilerServicesFacadeBaseAsync {

    suspend fun hasIncrementalCaches(): Boolean

    suspend fun hasLookupTracker(): Boolean

    suspend fun hasCompilationCanceledStatus(): Boolean

    // ----------------------------------------------------
    // IncrementalCache
    suspend fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String>

    suspend fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String>

    suspend fun incrementalCache_getPackagePartData(target: TargetId, partInternalName: String): JvmPackagePartProto?

    suspend fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray?

    suspend fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String)

    suspend fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String

    suspend fun incrementalCache_close(target: TargetId)

    suspend fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>?

    // ----------------------------------------------------
    // LookupTracker
    suspend fun lookupTracker_requiresPosition(): Boolean

    suspend fun lookupTracker_record(lookups: Collection<LookupInfo>)

    suspend fun lookupTracker_isDoNothing(): Boolean

    // ----------------------------------------------------
    // CompilationCanceledStatus
    suspend fun compilationCanceledStatus_checkCanceled(): Void?
}