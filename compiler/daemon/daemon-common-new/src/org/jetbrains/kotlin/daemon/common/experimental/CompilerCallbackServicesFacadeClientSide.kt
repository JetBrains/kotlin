/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.CompilerCallbackServicesFacadeAsync
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultClient
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import java.io.Serializable


interface CompilerCallbackServicesFacadeClientSide : CompilerCallbackServicesFacadeAsync, Client<CompilerServicesFacadeBaseServerSide>, CompilerServicesFacadeBaseClientSide

@Suppress("UNCHECKED_CAST")
class CompilerCallbackServicesFacadeClientSideImpl(serverPort: Int) : CompilerCallbackServicesFacadeClientSide,
    Client<CompilerServicesFacadeBaseServerSide> by DefaultClient(serverPort) {

    override suspend fun hasIncrementalCaches(): Boolean {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.HasIncrementalCachesMessage())
        return readMessage(id)
    }

    override suspend fun hasLookupTracker(): Boolean {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.HasLookupTrackerMessage())
        return readMessage(id)
    }

    override suspend fun hasCompilationCanceledStatus(): Boolean {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.HasCompilationCanceledStatusMessage())
        return readMessage(id)
    }

    override suspend fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String> {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getObsoletePackagePartsMessage(target))
        return readMessage(id)
    }

    override suspend fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String> {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getObsoleteMultifileClassFacadesMessage(target))
        return readMessage(id)
    }

    override suspend fun incrementalCache_getPackagePartData(target: TargetId, partInternalName: String): JvmPackagePartProto? {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getPackagePartDataMessage(target, partInternalName))
        return readMessage(id)
    }

    override suspend fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray? {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getModuleMappingDataMessage(target))
        return readMessage(id)
    }

    override suspend fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String) {
        sendNoReplyMessage(
            CompilerCallbackServicesFacadeServerSide.IncrementalCache_registerInlineMessage(
                target,
                fromPath,
                jvmSignature,
                toPath
            )
        )
    }

    override suspend fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getClassFilePathMessage(target, internalClassName))
        return readMessage(id)
    }

    override suspend fun incrementalCache_close(target: TargetId) =
        sendNoReplyMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_closeMessage(target))

    override suspend fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>? {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getMultifileFacadePartsMessage(target, internalName))
        return readMessage(id)
    }

    override suspend fun lookupTracker_requiresPosition(): Boolean {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.LookupTracker_requiresPositionMessage())
        return readMessage(id)
    }

    override suspend fun lookupTracker_record(lookups: Collection<LookupInfo>) =
        sendNoReplyMessage(CompilerCallbackServicesFacadeServerSide.LookupTracker_recordMessage(lookups))

    override suspend fun lookupTracker_isDoNothing(): Boolean {
        val id = sendMessage(CompilerCallbackServicesFacadeServerSide.LookupTracker_isDoNothingMessage())
        return readMessage(id)
    }

    override suspend fun compilationCanceledStatus_checkCanceled(): Void? {
        sendNoReplyMessage(CompilerCallbackServicesFacadeServerSide.CompilationCanceledStatus_checkCanceledMessage())
        return null
    }

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        sendNoReplyMessage(CompilerServicesFacadeBaseServerSide.ReportMessage(category, severity, message, attachment))
    }

}