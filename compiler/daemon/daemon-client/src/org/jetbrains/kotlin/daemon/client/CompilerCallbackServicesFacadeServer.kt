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

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.incremental.web.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.web.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.web.JsInlineFunctionHash
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.isProcessCanceledException
import java.io.File
import java.rmi.server.UnicastRemoteObject

open class CompilerCallbackServicesFacadeServer(
    val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
    val lookupTracker: LookupTracker? = null,
    val compilationCanceledStatus: CompilationCanceledStatus? = null,
    val expectActualTracker: ExpectActualTracker? = null,
    val inlineConstTracker: InlineConstTracker? = null,
    val enumWhenTracker: EnumWhenTracker? = null,
    val incrementalResultsConsumer: IncrementalResultsConsumer? = null,
    val incrementalDataProvider: IncrementalDataProvider? = null,
    port: Int = SOCKET_ANY_FREE_PORT
) : @Suppress("DEPRECATION") CompilerCallbackServicesFacade,
    UnicastRemoteObject(
        port,
        LoopbackNetworkInterface.clientLoopbackSocketFactory,
        LoopbackNetworkInterface.serverLoopbackSocketFactory
    ) {
    override fun hasIncrementalCaches(): Boolean = incrementalCompilationComponents != null

    override fun hasLookupTracker(): Boolean = lookupTracker != null

    override fun hasCompilationCanceledStatus(): Boolean = compilationCanceledStatus != null

    override fun hasExpectActualTracker(): Boolean = expectActualTracker != null

    override fun hasInlineConstTracker(): Boolean = inlineConstTracker != null

    override fun hasEnumWhenTracker(): Boolean = enumWhenTracker != null

    override fun hasIncrementalResultsConsumer(): Boolean = incrementalResultsConsumer != null

    override fun hasIncrementalDataProvider(): Boolean = incrementalDataProvider != null

    // TODO: consider replacing NPE with other reporting, although NPE here means most probably incorrect usage

    override fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String> =
        incrementalCompilationComponents!!.getIncrementalCache(target).getObsoletePackageParts()

    override fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String> =
        incrementalCompilationComponents!!.getIncrementalCache(target).getObsoleteMultifileClasses()

    override fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>? =
        incrementalCompilationComponents!!.getIncrementalCache(target).getStableMultifileFacadeParts(internalName)

    override fun incrementalCache_getPackagePartData(target: TargetId, partInternalName: String): JvmPackagePartProto? =
        incrementalCompilationComponents!!.getIncrementalCache(target).getPackagePartData(partInternalName)

    override fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray? =
        incrementalCompilationComponents!!.getIncrementalCache(target).getModuleMappingData()

    // todo: remove (the method it called was relevant only for old IC)
    override fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String) {
    }

    override fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String =
        incrementalCompilationComponents!!.getIncrementalCache(target).getClassFilePath(internalClassName)

    override fun incrementalCache_close(target: TargetId) {
        incrementalCompilationComponents!!.getIncrementalCache(target).close()
    }

    override fun lookupTracker_requiresPosition() = lookupTracker!!.requiresPosition

    override fun lookupTracker_record(lookups: Collection<LookupInfo>) {
        val lookupTracker = lookupTracker!!

        for (it in lookups) {
            lookupTracker.record(it.filePath, it.position, it.scopeFqName, it.scopeKind, it.name)
        }
    }

    private val lookupTracker_isDoNothing: Boolean = lookupTracker === LookupTracker.DO_NOTHING

    override fun lookupTracker_isDoNothing(): Boolean = lookupTracker_isDoNothing

    override fun compilationCanceledStatus_checkCanceled(): Void? {
        try {
            compilationCanceledStatus!!.checkCanceled()
            return null
        } catch (e: Exception) {
            // avoid passing exceptions that may have different serialVersionUID on across rmi border
            // removing dependency from openapi (this is obsolete part anyway, and will be removed soon)
            if (e.isProcessCanceledException())
                throw RmiFriendlyCompilationCanceledException()
            else throw e
        }
    }

    override fun expectActualTracker_report(expectedFilePath: String, actualFilePath: String) {
        expectActualTracker!!.report(File(expectedFilePath), File(actualFilePath))
    }

    override fun inlineConstTracker_report(filePath: String, owner: String, name: String, constType: String) {
        inlineConstTracker?.report(filePath, owner, name, constType) ?: throw NullPointerException("inlineConstTracker was not initialized")
    }

    override fun enumWhenTracker_report(whenUsageClassPath: String, enumClassFqName: String) {
        enumWhenTracker?.report(whenUsageClassPath, enumClassFqName) ?: throw NullPointerException("enumWhenTracker was not initialized")
    }

    override fun incrementalResultsConsumer_processHeader(headerMetadata: ByteArray) {
        incrementalResultsConsumer!!.processHeader(headerMetadata)
    }

    override fun incrementalResultsConsumer_processPackagePart(
        sourceFilePath: String,
        packagePartMetadata: ByteArray,
        binaryAst: ByteArray,
        inlineData: ByteArray
    ) {
        incrementalResultsConsumer!!.processPackagePart(File(sourceFilePath), packagePartMetadata, binaryAst, inlineData)
    }

    override fun incrementalResultsConsumer_processInlineFunctions(functions: Collection<JsInlineFunctionHash>) {
        incrementalResultsConsumer!!.processInlineFunctions(functions)
    }

    override fun incrementalResultsConsumer_processPackageMetadata(packageName: String, metadata: ByteArray) {
        incrementalResultsConsumer!!.processPackageMetadata(packageName, metadata)
    }

    override fun incrementalDataProvider_getHeaderMetadata(): ByteArray = incrementalDataProvider!!.headerMetadata

    override fun incrementalDataProvider_getMetadataVersion(): IntArray = incrementalDataProvider!!.metadataVersion

    override fun incrementalDataProvider_getCompiledPackageParts() =
        incrementalDataProvider!!.compiledPackageParts.entries.map {
            CompiledPackagePart(it.key.path, it.value.metadata, it.value.binaryAst, it.value.inlineData)
        }

    override fun incrementalDataProvider_getPackageMetadata(): Collection<PackageMetadata> =
        incrementalDataProvider!!.packageMetadata.entries.map { (fqName, metadata) ->
            PackageMetadata(fqName, metadata)
        }
}
