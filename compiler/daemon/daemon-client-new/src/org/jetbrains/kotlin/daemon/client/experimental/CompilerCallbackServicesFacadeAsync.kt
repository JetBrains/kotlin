/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.reportFromDaemon
import org.jetbrains.kotlin.daemon.common.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ServerSocketWrapper

import org.jetbrains.kotlin.incremental.components.LookupInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.load.kotlin.incremental.components.JvmPackagePartProto
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.progress.experimental.CompilationCanceledStatus
import org.jetbrains.kotlin.utils.isProcessCanceledException
import java.io.Serializable

open class CompilerCallbackServicesFacadeServerServerSide(
    val incrementalCompilationComponents: IncrementalCompilationComponents? = null,
    val lookupTracker: LookupTracker? = null,
    val compilationCanceledStatus: CompilationCanceledStatus? = null,
    val messageCollector: MessageCollector? = null,
    override val serverSocketWithPort: ServerSocketWrapper = findCallbackServerSocket()
) : CompilerCallbackServicesFacadeServerSide {

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        messageCollector?.reportFromDaemon(null, category, severity, message, attachment)
    }

    override val clients = hashMapOf<Socket, Server.ClientInfo>()

    val clientSide : CompilerServicesFacadeBaseClientSide
        get() = CompilerCallbackServicesFacadeClientSideImpl(serverSocketWithPort.port)

    override suspend fun hasIncrementalCaches(): Boolean = incrementalCompilationComponents != null

    override suspend fun hasLookupTracker(): Boolean = lookupTracker != null

    override suspend fun hasCompilationCanceledStatus(): Boolean = compilationCanceledStatus != null

    // TODO: consider replacing NPE with other reporting, although NPE here means most probably incorrect usage

    override suspend fun incrementalCache_getObsoletePackageParts(target: TargetId): Collection<String> =
        incrementalCompilationComponents!!.getIncrementalCache(target).getObsoletePackageParts()

    override suspend fun incrementalCache_getObsoleteMultifileClassFacades(target: TargetId): Collection<String> =
        incrementalCompilationComponents!!.getIncrementalCache(target).getObsoleteMultifileClasses()

    override suspend fun incrementalCache_getMultifileFacadeParts(target: TargetId, internalName: String): Collection<String>? =
        incrementalCompilationComponents!!.getIncrementalCache(target).getStableMultifileFacadeParts(internalName)

    override suspend fun incrementalCache_getPackagePartData(target: TargetId, partInternalName: String): JvmPackagePartProto? =
        incrementalCompilationComponents!!.getIncrementalCache(target).getPackagePartData(partInternalName)

    override suspend fun incrementalCache_getModuleMappingData(target: TargetId): ByteArray? =
        incrementalCompilationComponents!!.getIncrementalCache(target).getModuleMappingData()

    // todo: remove (the method it called was relevant only for old IC)
    override suspend fun incrementalCache_registerInline(target: TargetId, fromPath: String, jvmSignature: String, toPath: String) {
    }

    override suspend fun incrementalCache_getClassFilePath(target: TargetId, internalClassName: String): String =
        incrementalCompilationComponents!!.getIncrementalCache(target).getClassFilePath(internalClassName)

    override suspend fun incrementalCache_close(target: TargetId) {
        incrementalCompilationComponents!!.getIncrementalCache(target).close()
    }

    override suspend fun lookupTracker_requiresPosition() = lookupTracker!!.requiresPosition

    override suspend fun lookupTracker_record(lookups: Collection<LookupInfo>) {
        val lookupTracker = lookupTracker!!

        for (it in lookups) {
            lookupTracker.record(it.filePath, it.position, it.scopeFqName, it.scopeKind, it.name)
        }
    }

    private val lookupTracker_isDoNothing: Boolean = lookupTracker === LookupTracker.DO_NOTHING

    override suspend fun lookupTracker_isDoNothing(): Boolean = lookupTracker_isDoNothing

    override suspend fun compilationCanceledStatus_checkCanceled(): Void? {
        try {
            compilationCanceledStatus?.checkCanceled()
            return null
        }
        catch (e: Exception) {
            // avoid passing exceptions that may have different serialVersionUID on across rmi border
            // removing dependency from openapi (this is obsolete part anyway, and will be removed soon)
            if (e.isProcessCanceledException())
                throw Exception("-TODO- RmiFriendlyCompilationCanceledException()") //RmiFriendlyCompilationCanceledException()
            else throw e
        }
    }

}

