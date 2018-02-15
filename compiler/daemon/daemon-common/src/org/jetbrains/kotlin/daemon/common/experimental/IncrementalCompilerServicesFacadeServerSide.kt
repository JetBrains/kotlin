/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import java.io.File


interface IncrementalCompilerServicesFacadeServerSide : IncrementalCompilerServicesFacadeAsync, CompilerServicesFacadeBaseServerSide {

    // Query messages:

    class HasAnnotationsFileUpdaterMessage : Server.Message<IncrementalCompilerServicesFacadeServerSide> {
        override suspend fun process(server: IncrementalCompilerServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.hasAnnotationsFileUpdater())
    }

    class UpdateAnnotationsMessage(val outdatedClassesJvmNames: Iterable<String>) :
        Server.Message<IncrementalCompilerServicesFacadeServerSide> {
        override suspend fun process(server: IncrementalCompilerServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.updateAnnotations(outdatedClassesJvmNames))
    }

    class RevertMessage(val outdatedClassesJvmNames: Iterable<String>) : Server.Message<IncrementalCompilerServicesFacadeServerSide> {
        override suspend fun process(server: IncrementalCompilerServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            server.revert()
    }

    class RegisterChangesMessage(val timestamp: Long, val dirtyData: SimpleDirtyData) :
        Server.Message<IncrementalCompilerServicesFacadeServerSide> {
        override suspend fun process(server: IncrementalCompilerServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            server.registerChanges(timestamp, dirtyData)
    }

    class UnknownChangesMessage(val timestamp: Long) : Server.Message<IncrementalCompilerServicesFacadeServerSide> {
        override suspend fun process(server: IncrementalCompilerServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            server.unknownChanges(timestamp)
    }

    class GetChangesMessage(
        val artifact: File,
        val sinceTS: Long
    ) : Server.Message<IncrementalCompilerServicesFacadeServerSide> {
        override suspend fun process(server: IncrementalCompilerServicesFacadeServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(server.getChanges(artifact, sinceTS))
    }

}