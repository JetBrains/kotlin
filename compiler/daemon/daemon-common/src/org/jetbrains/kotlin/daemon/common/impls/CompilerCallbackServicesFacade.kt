/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.impls

import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.js.JsInlineFunctionHash
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException


/**
 * common facade for compiler services exposed from client process (e.g. JPS) to the compiler running on daemon
 * the reason for having common facade is attempt to reduce number of connections between client and daemon
 * Note: non-standard naming convention used to denote combining several entities in one facade - prefix <entityName>_ is used for every function belonging to the entity
 */
@Deprecated(
    "The usages should be replaced with `compile` method and `CompilerServicesFacadeBase` implementations",
    ReplaceWith("CompilerServicesFacadeBase")
)
interface CompilerCallbackServicesFacade : Remote {

    @Throws(RemoteException::class)
    fun hasIncrementalCaches(): Boolean

    @Throws(RemoteException::class)
    fun hasLookupTracker(): Boolean

    @Throws(RemoteException::class)
    fun hasCompilationCanceledStatus(): Boolean

    @Throws(RemoteException::class)
    fun hasExpectActualTracker(): Boolean

    @Throws(RemoteException::class)
    fun hasIncrementalResultsConsumer(): Boolean

    @Throws(RemoteException::class)
    fun hasIncrementalDataProvider(): Boolean

    // ----------------------------------------------------
    // IncrementalCache
    @Throws(RemoteException::class)
    fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String>

    @Throws(RemoteException::class)
    fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String>

    @Throws(RemoteException::class)
    fun incrementalCache_getPackagePartData(target: TargetId, partInternalName: String): JvmPackagePartProto?

    @Throws(RemoteException::class)
    fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray?

    @Throws(RemoteException::class)
    fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String)

    @Throws(RemoteException::class)
    fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String

    @Throws(RemoteException::class)
    fun incrementalCache_close(target: TargetId)

    @Throws(RemoteException::class)
    fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>?

    // ----------------------------------------------------
    // LookupTracker
    @Throws(RemoteException::class)
    fun lookupTracker_requiresPosition(): Boolean

    @Throws(RemoteException::class)
    fun lookupTracker_record(lookups: Collection<LookupInfo>)

    @Throws(RemoteException::class)
    fun lookupTracker_isDoNothing(): Boolean

    // ----------------------------------------------------
    // CompilationCanceledStatus
    @Throws(RemoteException::class, RmiFriendlyCompilationCanceledException::class)
    fun compilationCanceledStatus_checkCanceled(): Void?

    // ---------------------------------------------------
    // ExpectActualTracker
    @Throws(RemoteException::class)
    fun expectActualTracker_report(expectedFilePath: String, actualFilePath: String)

    // ---------------------------------------------------
    // IncrementalResultsConsumer (js)
    @Throws(RemoteException::class)
    fun incrementalResultsConsumer_processHeader(headerMetadata: ByteArray)

    @Throws(RemoteException::class)
    fun incrementalResultsConsumer_processPackagePart(sourceFilePath: String, packagePartMetadata: ByteArray, binaryAst: ByteArray)

    @Throws(RemoteException::class)
    fun incrementalResultsConsumer_processInlineFunctions(functions: Collection<JsInlineFunctionHash>)

    // ---------------------------------------------------
    // IncrementalDataProvider (js)
    @Throws(RemoteException::class)
    fun incrementalDataProvider_getHeaderMetadata(): ByteArray

    @Throws(RemoteException::class)
    fun incrementalDataProvider_getCompiledPackageParts(): Collection<CompiledPackagePart>

    @Throws(RemoteException::class)
    fun incrementalDataProvider_getMetadataVersion(): IntArray
}

class CompiledPackagePart(
    val filePath: String,
    val metadata: ByteArray, val binaryAst: ByteArray
) : Serializable

class RmiFriendlyCompilationCanceledException : Exception(), Serializable {
    companion object {
        // just a random number, but should never be changed to avoid deserialization problems
        private val serialVersionUID: Long = 8228357578L
    }
}
