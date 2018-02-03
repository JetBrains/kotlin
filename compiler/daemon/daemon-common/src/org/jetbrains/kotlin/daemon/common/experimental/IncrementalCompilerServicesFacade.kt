/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import java.io.File
import java.rmi.RemoteException
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper


interface IncrementalCompilerServicesFacade : CompilerServicesFacadeBase {
    // AnnotationFileUpdater
    @Throws(RemoteException::class)
    fun hasAnnotationsFileUpdater(): Boolean

    @Throws(RemoteException::class)
    fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>)

    @Throws(RemoteException::class)
    fun revert()

    // ChangesRegistry
    @Throws(RemoteException::class)
    fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData)

    @Throws(RemoteException::class)
    fun unknownChanges(timestamp: Long)

    @Throws(RemoteException::class)
    fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>?

    // Query messages:
    /*
    class HasAnnotationsFileUpdaterMessage : Message<IncrementalCompilerServicesFacade> {
        suspend override fun process(server: IncrementalCompilerServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.hasAnnotationsFileUpdater())
    }

    class UpdateAnnotationsMessage(val outdatedClassesJvmNames: Iterable<String>) : Message<IncrementalCompilerServicesFacade> {
        suspend override fun process(server: IncrementalCompilerServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.updateAnnotations(outdatedClassesJvmNames))
    }

    class RevertMessage(val outdatedClassesJvmNames: Iterable<String>): Message<IncrementalCompilerServicesFacade> {
        suspend override fun process(server: IncrementalCompilerServicesFacade, output: ByteWriteChannelWrapper) =
            server.revert()
    }

    class RegisterChangesMessage(val timestamp: Long, val dirtyData: SimpleDirtyData): Message<IncrementalCompilerServicesFacade> {
        suspend override fun process(server: IncrementalCompilerServicesFacade, output: ByteWriteChannelWrapper) =
            server.registerChanges(timestamp, dirtyData)
    }

    class UnknownChangesMessage(val timestamp: Long): Message<IncrementalCompilerServicesFacade> {
        suspend override fun process(server: IncrementalCompilerServicesFacade, output: ByteWriteChannelWrapper) =
            server.unknownChanges(timestamp)
    }

    class GetChangesMessage(
        val artifact: File,
        val sinceTS: Long
    ): Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getChanges(artifact, sinceTS))
    }
    */

}