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
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.js.IncrementalResultsConsumer
import org.jetbrains.kotlin.incremental.js.JsInlineFunctionHash
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
    val incrementalResultsConsumer: IncrementalResultsConsumer? = null,
    val incrementalDataProvider: IncrementalDataProvider? = null,
    port: Int = SOCKET_ANY_FREE_PORT
) : @Suppress("DEPRECATION") CompilerCallbackServicesFacade,
    UnicastRemoteObject(
        port,
        LoopbackNetworkInterface.clientLoopbackSocketFactory,
        LoopbackNetworkInterface.serverLoopbackSocketFactory
    ) {
    var stat = mutableMapOf<String, Int>()
    override fun hasIncrementalCaches(): Boolean {
        stat["hasIncrementalCaches"] = (stat["hasIncrementalCaches"] ?: 0) + 1
        return incrementalCompilationComponents != null
    }

    override fun hasLookupTracker(): Boolean {
        stat["hasLookupTracker"] = (stat["hasLookupTracker"] ?: 0) + 1
        return lookupTracker != null
    }

    override fun hasCompilationCanceledStatus(): Boolean {
        stat["hasCompilationCanceledStatus"] = (stat["hasCompilationCanceledStatus"] ?: 0) + 1
        return compilationCanceledStatus != null
    }

    override fun hasExpectActualTracker(): Boolean {
        stat["hasExpectActualTracker"] = (stat["hasExpectActualTracker"] ?: 0) + 1
        return expectActualTracker != null
    }

    override fun hasIncrementalResultsConsumer(): Boolean {
        stat["hasIncrementalResultsConsumer"] = (stat["hasIncrementalResultsConsumer"] ?: 0) + 1
        return incrementalResultsConsumer != null
    }

    override fun hasIncrementalDataProvider(): Boolean {
        stat["hasIncrementalDataProvider"] = (stat["hasIncrementalDataProvider"] ?: 0) + 1
        return incrementalDataProvider != null
    }

    // TODO: consider replacing NPE with other reporting, although NPE here means most probably incorrect usage

    override fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String> {
        stat["incrementalCache_getObsoletePackageParts"] = (stat["incrementalCache_getObsoletePackageParts"] ?: 0) + 1
        return incrementalCompilationComponents!!.getIncrementalCache(target).getObsoletePackageParts()
    }

    override fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String> {
        stat["incrementalCache_getObsoleteMultifileClassFacades"] = (stat["incrementalCache_getObsoleteMultifileClassFacades"] ?: 0) + 1
        return incrementalCompilationComponents!!.getIncrementalCache(target).getObsoleteMultifileClasses()
    }

    override fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>? {
        stat["incrementalCache_getMultifileFacadeParts"] = (stat["incrementalCache_getMultifileFacadeParts"] ?: 0) + 1
        return incrementalCompilationComponents!!.getIncrementalCache(target).getStableMultifileFacadeParts(internalName)
    }

    override fun incrementalCache_getPackagePartData(target: TargetId, partInternalName: String): JvmPackagePartProto? {
        stat["incrementalCache_getPackagePartData"] = (stat["incrementalCache_getPackagePartData"] ?: 0) + 1
        return incrementalCompilationComponents!!.getIncrementalCache(target).getPackagePartData(partInternalName)
    }

    override fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray? {
        stat["incrementalCache_getModuleMappingData"] = (stat["incrementalCache_getModuleMappingData"] ?: 0) + 1
        return incrementalCompilationComponents!!.getIncrementalCache(target).getModuleMappingData()
    }

    // todo: remove (the method it called was relevant only for old IC)
    override fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String) {
        stat["incrementalCache_registerInline"] = (stat["incrementalCache_registerInline"] ?: 0) + 1
    }

    override fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String {
        stat["incrementalCache_getClassFilePath"] = (stat["incrementalCache_getClassFilePath"] ?: 0) + 1
        return incrementalCompilationComponents!!.getIncrementalCache(target).getClassFilePath(internalClassName)
    }

    override fun incrementalCache_close(target: TargetId) {
        stat["incrementalCache_close"] = (stat["incrementalCache_close"] ?: 0) + 1
        incrementalCompilationComponents!!.getIncrementalCache(target).close()
    }

    override fun lookupTracker_requiresPosition(): Boolean {
        stat["lookupTracker_requiresPosition"] = (stat["lookupTracker_requiresPosition"] ?: 0) + 1
        return lookupTracker!!.requiresPosition
    }

    override fun lookupTracker_record(lookups: Collection<LookupInfo>) {
        stat["lookupTracker_record"] = (stat["lookupTracker_record"] ?: 0) + 1
        val lookupTracker = lookupTracker!!

        for (it in lookups) {
            lookupTracker.record(it.filePath, it.position, it.scopeFqName, it.scopeKind, it.name)
        }
    }

    private val lookupTracker_isDoNothing: Boolean = lookupTracker === LookupTracker.DO_NOTHING

    override fun lookupTracker_isDoNothing(): Boolean {
        stat["lookupTracker_isDoNothing"] = (stat["lookupTracker_isDoNothing"] ?: 0) + 1
        return lookupTracker_isDoNothing
    }

    override fun compilationCanceledStatus_checkCanceled(): Void? {
        stat["compilationCanceledStatus_checkCanceled"] = (stat["compilationCanceledStatus_checkCanceled"] ?: 0) + 1
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
        stat["expectActualTracker_report"] = (stat["expectActualTracker_report"] ?: 0) + 1
        expectActualTracker!!.report(File(expectedFilePath), File(actualFilePath))
    }

    override fun incrementalResultsConsumer_processHeader(headerMetadata: ByteArray) {
        stat["incrementalResultsConsumer_processHeader"] = (stat["incrementalResultsConsumer_processHeader"] ?: 0) + 1
        incrementalResultsConsumer!!.processHeader(headerMetadata)
    }

    override fun incrementalResultsConsumer_processPackagePart(
        sourceFilePath: String,
        packagePartMetadata: ByteArray,
        binaryAst: ByteArray,
        inlineData: ByteArray
    ) {
        stat["incrementalResultsConsumer_processPackagePart"] = (stat["incrementalResultsConsumer_processPackagePart"] ?: 0) + 1
        incrementalResultsConsumer!!.processPackagePart(File(sourceFilePath), packagePartMetadata, binaryAst, inlineData)
    }

    override fun incrementalResultsConsumer_processInlineFunctions(functions: Collection<JsInlineFunctionHash>) {
        stat["incrementalResultsConsumer_processInlineFunctions"] = (stat["incrementalResultsConsumer_processInlineFunctions"] ?: 0) + 1
        incrementalResultsConsumer!!.processInlineFunctions(functions)
    }

    override fun incrementalResultsConsumer_processPackageMetadata(packageName: String, metadata: ByteArray) {
        stat["incrementalResultsConsumer_processPackageMetadata"] = (stat["incrementalResultsConsumer_processPackageMetadata"] ?: 0) + 1
        incrementalResultsConsumer!!.processPackageMetadata(packageName, metadata)
    }

    override fun incrementalDataProvider_getHeaderMetadata(): ByteArray {
        stat["incrementalDataProvider_getHeaderMetadata"] = (stat["incrementalDataProvider_getHeaderMetadata"] ?: 0) + 1
        return incrementalDataProvider!!.headerMetadata
    }

    override fun incrementalDataProvider_getMetadataVersion(): IntArray {
        stat["incrementalDataProvider_getMetadataVersion"] = (stat["incrementalDataProvider_getMetadataVersion"] ?: 0) + 1
        return incrementalDataProvider!!.metadataVersion
    }

    override fun incrementalDataProvider_getCompiledPackageParts(): List<CompiledPackagePart> {
        stat["incrementalDataProvider_getCompiledPackageParts"] = (stat["incrementalDataProvider_getCompiledPackageParts"] ?: 0) + 1
        return incrementalDataProvider!!.compiledPackageParts.entries.map {
            CompiledPackagePart(it.key.path, it.value.metadata, it.value.binaryAst, it.value.inlineData)
        }
    }

    override fun incrementalDataProvider_getPackageMetadata(): Collection<PackageMetadata> {
        stat["incrementalDataProvider_getPackageMetadata"] = (stat["incrementalDataProvider_getPackageMetadata"] ?: 0) + 1
        return incrementalDataProvider!!.packageMetadata.entries.map { (fqName, metadata) ->
            PackageMetadata(fqName, metadata)
        }
    }
}
