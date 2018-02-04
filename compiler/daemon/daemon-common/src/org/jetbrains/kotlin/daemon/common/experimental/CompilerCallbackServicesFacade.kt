/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import org.jetbrains.kotlin.incremental.components.LookupInfo
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
    //TODO delete??? : @Throws(RmiFriendlyCompilationCanceledException::class)
    @Throws(RemoteException::class, RmiFriendlyCompilationCanceledException::class)
    fun compilationCanceledStatus_checkCanceled(): Void?

}

interface CompilerCallbackServicesFacadeClientSide : CompilerCallbackServicesFacade, Client

interface CompilerCallbackServicesFacadeServerSide : CompilerCallbackServicesFacade, Server {

    // Query-messages:

    class HasIncrementalCachesMessage : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.hasCompilationCanceledStatus())
    }

    class HasLookupTrackerMessage : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.hasLookupTracker())
    }

    class HasCompilationCanceledStatusMessage : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.hasCompilationCanceledStatus())
    }

    class IncrementalCache_getObsoletePackagePartsMessage(val target: TargetId) : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.incrementalCache_getObsoletePackageParts(target))
    }

    class IncrementalCache_getObsoleteMultifileClassFacadesMessage(val target: TargetId) : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.incrementalCache_getObsoleteMultifileClassFacades(target))
    }

    class IncrementalCache_getPackagePartDataMessage(
        val target: TargetId,
        val partInternalName: String
    ) : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.incrementalCache_getPackagePartData(target, partInternalName))
    }

    class IncrementalCache_getModuleMappingDataMessage(val target: TargetId) : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.incrementalCache_getModuleMappingData(target))
    }

    class IncrementalCache_registerInlineMessage(
        val target: TargetId,
        val fromPath: String,
        val jvmSignature: String,
        val toPath: String
    ) : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            server.incrementalCache_registerInline(
                target,
                fromPath,
                jvmSignature,
                toPath
            )
    }

    class IncrementalCache_getClassFilePathMessage(
        val target: TargetId,
        val internalClassName: String
    ) : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.incrementalCache_getClassFilePath(target, internalClassName))
    }

    class IncrementalCache_closeMessage(val target: TargetId) : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            server.incrementalCache_close(target)
    }

    class IncrementalCache_getMultifileFacadePartsMessage(
        val target: TargetId,
        val internalName: String
    ) : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.incrementalCache_getMultifileFacadeParts(target, internalName))
    }

    class LookupTracker_requiresPositionMessage : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.lookupTracker_requiresPosition())
    }

    class LookupTracker_recordMessage(val lookups: Collection<LookupInfo>) : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.lookupTracker_record(lookups))
    }

    class LookupTracker_isDoNothingMessage : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.lookupTracker_isDoNothing())
    }

    class CompilationCanceledStatus_checkCanceledMessage : Message<CompilerCallbackServicesFacadeServerSide> {
        override suspend fun process(server: CompilerCallbackServicesFacadeServerSide, output: ByteWriteChannelWrapper) {
            server.compilationCanceledStatus_checkCanceled()
        }
    }

}

// TODO delete???
class RmiFriendlyCompilationCanceledException : Exception(), Serializable {
    companion object {
        private val serialVersionUID: Long =
            8228357578L // just a random number, but should never be changed to avoid deserialization problems
    }
}
