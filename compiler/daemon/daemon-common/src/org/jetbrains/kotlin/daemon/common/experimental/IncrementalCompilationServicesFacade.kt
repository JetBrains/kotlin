/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import java.io.File
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException

interface IncrementalCompilationServicesFacade : Remote {
    @Throws(RemoteException::class)
    fun areFileChangesKnown(): Boolean

    @Throws(RemoteException::class)
    fun modifiedFiles(): List<File>?

    @Throws(RemoteException::class)
    fun deletedFiles(): List<File>?

    @Throws(RemoteException::class)
    fun workingDir(): File

    @Throws(RemoteException::class)
    fun customCacheVersionFileName(): String

    @Throws(RemoteException::class)
    fun customCacheVersion(): Int

    // ICReporter

    @Throws(RemoteException::class)
    fun shouldReportIC(): Boolean

    @Throws(RemoteException::class)
    fun reportIC(message: String)

    @Throws(RemoteException::class)
    fun reportCompileIteration(files: Iterable<File>, exitCode: Int)

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
    class AreFileChangesKnownMessage: Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.areFileChangesKnown())
    }

    class ModifiedFilesMessage: Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.modifiedFiles())
    }

    class DeletedFilesMessage: Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.deletedFiles())
    }

    class WorkingDirMessage: Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.workingDir())
    }

    class CustomCacheVersionFileNameMessage: Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.customCacheVersionFileName())
    }

    class CustomCacheVersionMessage: Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.customCacheVersion())
    }

    class ReportICMessage(val message: String): Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.reportIC(message)
    }

    class ReportCompileIterationMessage(
        val files: Iterable<File>,
        val exitCode: Int
    ): Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.reportCompileIteration(files, exitCode))
    }

    class UpdateAnnotationsMessage(val outdatedClassesJvmNames: Iterable<String>): Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.updateAnnotations(outdatedClassesJvmNames)
    }

    class RevertMessage: Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.revert()
    }

    class RegisterChangesMessage(val timestamp: Long, val dirtyData: SimpleDirtyData): Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.registerChanges(timestamp, dirtyData)
    }

    class UnknownChangesMessage(val timestamp: Long): Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.unknownChanges(timestamp)
    }

    class GetChangesMessage(
        val artifact: File,
        val sinceTS: Long
    ): Message<IncrementalCompilationServicesFacade> {
        override suspend fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getChanges(artifact, sinceTS))
    }*/

}