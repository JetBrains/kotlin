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

import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.web.WebInlineFunctionHash
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
    fun hasInlineConstTracker(): Boolean

    @Throws(RemoteException::class)
    fun hasEnumWhenTracker(): Boolean

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
    // InlineConstTracker
    @Throws(RemoteException::class)
    fun inlineConstTracker_report(filePath: String, owner: String, name: String, constType: String)

    // ---------------------------------------------------
    // EnumWhenTracker
    @Throws(RemoteException::class)
    fun enumWhenTracker_report(whenUsageClassPath: String, enumClassFqName: String)

    // ---------------------------------------------------
    // IncrementalResultsConsumer (js)
    @Throws(RemoteException::class)
    fun incrementalResultsConsumer_processHeader(headerMetadata: ByteArray)

    @Throws(RemoteException::class)
    fun incrementalResultsConsumer_processPackagePart(sourceFilePath: String, packagePartMetadata: ByteArray, binaryAst: ByteArray, inlineData: ByteArray)

    @Throws(RemoteException::class)
    fun incrementalResultsConsumer_processInlineFunctions(functions: Collection<WebInlineFunctionHash>)

    @Throws(RemoteException::class)
    fun incrementalResultsConsumer_processPackageMetadata(packageName: String, metadata: ByteArray)

    // ---------------------------------------------------
    // IncrementalDataProvider (js)
    @Throws(RemoteException::class)
    fun incrementalDataProvider_getHeaderMetadata(): ByteArray

    @Throws(RemoteException::class)
    fun incrementalDataProvider_getCompiledPackageParts(): Collection<CompiledPackagePart>

    @Throws(RemoteException::class)
    fun incrementalDataProvider_getMetadataVersion(): IntArray

    @Throws(RemoteException::class)
    fun incrementalDataProvider_getPackageMetadata(): Collection<PackageMetadata>
}

class CompiledPackagePart(
    val filePath: String,
    val metadata: ByteArray, val binaryAst: ByteArray, val inlineData: ByteArray
) : Serializable

class PackageMetadata(
    val packageName: String,
    val metadata: ByteArray
) : Serializable {
    companion object {
        // just a random number, but should never be changed to avoid deserialization problems
        private val serialVersionUID: Long = 54021986502349756L
    }
}


class RmiFriendlyCompilationCanceledException : Exception(), Serializable {
    companion object {
        // just a random number, but should never be changed to avoid deserialization problems
        private val serialVersionUID: Long = 8228357578L
    }
}
