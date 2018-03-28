/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*
import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import java.beans.Transient
import java.io.Serializable
import java.net.InetSocketAddress


interface CompilerCallbackServicesFacadeClientSide : CompilerCallbackServicesFacadeAsync, Client, CompilerServicesFacadeBaseClientSide

@Suppress("UNCHECKED_CAST")
class CompilerCallbackServicesFacadeClientSideImpl(serverPort: Int) : CompilerCallbackServicesFacadeClientSide, Client by DefaultClient(serverPort) {

    override suspend fun hasIncrementalCaches(): Boolean {
        sendMessage(CompilerCallbackServicesFacadeServerSide.HasIncrementalCachesMessage()).await()
        return readMessage<Boolean>().await()
    }

    override suspend fun hasLookupTracker(): Boolean {
        sendMessage(CompilerCallbackServicesFacadeServerSide.HasLookupTrackerMessage()).await()
        return readMessage<Boolean>().await()
    }

    override suspend fun hasCompilationCanceledStatus(): Boolean {
        sendMessage(CompilerCallbackServicesFacadeServerSide.HasCompilationCanceledStatusMessage()).await()
        return readMessage<Boolean>().await()
    }

    override suspend fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String> {
        sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getObsoletePackagePartsMessage(target)).await()
        return readMessage<Collection<String>>().await()
    }

    override suspend fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String> {
        sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getObsoleteMultifileClassFacadesMessage(target)).await()
        return readMessage<Collection<String>>().await()
    }

    override suspend fun incrementalCache_getPackagePartData(target: TargetId, partInternalName: String): JvmPackagePartProto? {
        sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getPackagePartDataMessage(target, partInternalName)).await()
        return readMessage<JvmPackagePartProto?>().await()
    }

    override suspend fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray? {
        sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getModuleMappingDataMessage(target)).await()
        return readMessage<ByteArray?>().await()
    }

    override suspend fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String) =
        sendMessage(
            CompilerCallbackServicesFacadeServerSide.IncrementalCache_registerInlineMessage(
                target,
                fromPath,
                jvmSignature,
                toPath
            )
        ).await()

    override suspend fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String {
        sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getClassFilePathMessage(target, internalClassName)).await()
        return readMessage<String>().await()
    }

    override suspend fun incrementalCache_close(target: TargetId) =
        sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_closeMessage(target)).await()

    override suspend fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>? {
        sendMessage(CompilerCallbackServicesFacadeServerSide.IncrementalCache_getMultifileFacadePartsMessage(target, internalName)).await()
        return readMessage<Collection<String>?>().await()
    }

    override suspend fun lookupTracker_requiresPosition(): Boolean {
        sendMessage(CompilerCallbackServicesFacadeServerSide.LookupTracker_requiresPositionMessage()).await()
        return readMessage<Boolean>().await()
    }

    override suspend fun lookupTracker_record(lookups: Collection<LookupInfo>) =
        sendMessage(CompilerCallbackServicesFacadeServerSide.LookupTracker_recordMessage(lookups)).await()

    override suspend fun lookupTracker_isDoNothing(): Boolean {
        sendMessage(CompilerCallbackServicesFacadeServerSide.LookupTracker_isDoNothingMessage()).await()
        return readMessage<Boolean>().await()
    }

    override suspend fun compilationCanceledStatus_checkCanceled(): Void? {
        sendMessage(CompilerCallbackServicesFacadeServerSide.CompilationCanceledStatus_checkCanceledMessage()).await()
        return null
    }

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        sendMessage(CompilerServicesFacadeBaseServerSide.ReportMessage(category, severity, message, attachment))
    }

}