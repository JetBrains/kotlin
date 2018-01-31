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

package org.jetbrains.kotlin.daemon.experimental.common

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server.Message
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
@Deprecated("The usages should be replaced with `compile` method and `CompilerServicesFacadeBase` implementations", ReplaceWith("CompilerServicesFacadeBase"))
interface CompilerCallbackServicesFacade : Remote, Server {

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




    // Query-messages:

    class HasIncrementalCachesMessage : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.hasCompilationCanceledStatus())
    }

    class HasLookupTrackerMessage : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.hasLookupTracker())
    }

    class HasCompilationCanceledStatusMessage : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.hasCompilationCanceledStatus())
    }

    class IncrementalCache_getObsoletePackagePartsMessage(val target: TargetId)
        : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.incrementalCache_getObsoletePackageParts(target))
    }

    class IncrementalCache_getObsoleteMultifileClassFacadesMessage(val target: TargetId)
        : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.incrementalCache_getObsoleteMultifileClassFacades(target))
    }

    class IncrementalCache_getPackagePartDataMessage(
            val target: TargetId,
            val partInternalName: String
    ) : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.incrementalCache_getPackagePartData(target, partInternalName))
    }

    class IncrementalCache_getModuleMappingDataMessage(val target: TargetId)
        : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.incrementalCache_getModuleMappingData(target))
    }

    class IncrementalCache_registerInlineMessage(
            val target: TargetId,
            val fromPath: String,
            val jvmSignature: String,
            val toPath: String
    ) : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
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
    ) : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) = server.send(
                clientSocket,
                server.incrementalCache_getClassFilePath(target, internalClassName)
        )
    }

    class IncrementalCache_closeMessage(val target: TargetId) : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.incrementalCache_close(target)
    }

    class IncrementalCache_getMultifileFacadePartsMessage(
            val target: TargetId,
            val internalName: String
    ) : Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.incrementalCache_getMultifileFacadeParts(target, internalName))
    }

    class LookupTracker_requiresPositionMessage: Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.lookupTracker_requiresPosition())
    }

    class LookupTracker_recordMessage(val lookups: Collection<LookupInfo>): Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.lookupTracker_record(lookups))
    }

    class LookupTracker_isDoNothingMessage: Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) =
                server.send(clientSocket, server.lookupTracker_isDoNothing())
    }

    class CompilationCanceledStatus_checkCanceledMessage: Message<CompilerCallbackServicesFacade> {
        override suspend fun process(server: CompilerCallbackServicesFacade, clientSocket: Socket) {
            server.compilationCanceledStatus_checkCanceled()
        }
    }

}

// TODO delete???
class RmiFriendlyCompilationCanceledException : Exception(), Serializable {
    companion object {
        private val serialVersionUID: Long = 8228357578L // just a random number, but should never be changed to avoid deserialization problems
    }
}
