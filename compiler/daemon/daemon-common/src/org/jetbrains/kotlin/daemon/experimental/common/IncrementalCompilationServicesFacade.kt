/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.ByteWriteChannelWrapper
import java.io.File
import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server.Message

interface IncrementalCompilationServicesFacade : Server, Remote {
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

    class AreFileChangesKnownMessage: Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.areFileChangesKnown())
    }

    class ModifiedFilesMessage: Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.modifiedFiles())
    }

    class DeletedFilesMessage: Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.deletedFiles())
    }

    class WorkingDirMessage: Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.workingDir())
    }

    class CustomCacheVersionFileNameMessage: Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.customCacheVersionFileName())
    }

    class CustomCacheVersionMessage: Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.customCacheVersion())
    }

    class ReportICMessage(val message: String): Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.reportIC(message)
    }

    class ReportCompileIterationMessage(
        val files: Iterable<File>,
        val exitCode: Int
    ): Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.reportCompileIteration(files, exitCode))
    }

    class UpdateAnnotationsMessage(val outdatedClassesJvmNames: Iterable<String>): Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.updateAnnotations(outdatedClassesJvmNames)
    }

    class RevertMessage: Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.revert()
    }

    class RegisterChangesMessage(val timestamp: Long, val dirtyData: SimpleDirtyData): Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.registerChanges(timestamp, dirtyData)
    }

    class UnknownChangesMessage(val timestamp: Long): Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            server.unknownChanges(timestamp)
    }

    class GetChangesMessage(
        val artifact: File,
        val sinceTS: Long
    ): Message<IncrementalCompilationServicesFacade> {
        suspend override fun process(server: IncrementalCompilationServicesFacade, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getChanges(artifact, sinceTS))
    }

}

class SimpleDirtyData(
        val dirtyLookupSymbols: List<String>,
        val dirtyClassesFqNames: List<String>
) : Serializable