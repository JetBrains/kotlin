/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.async
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*
import java.beans.Transient
import java.io.File
import java.io.Serializable
import java.net.InetSocketAddress
import org.jetbrains.kotlin.daemon.common.experimental.IncrementalCompilerServicesFacadeServerSide.*

interface IncrementalCompilerServicesFacadeClientSide : IncrementalCompilerServicesFacadeAsync, CompilerServicesFacadeBaseClientSide

class IncrementalCompilerServicesFacadeClientSideImpl(val serverPort: Int) :
    IncrementalCompilerServicesFacadeClientSide,
    Client<CompilerServicesFacadeBaseServerSide> by DefaultClient(serverPort) {

    override suspend fun hasAnnotationsFileUpdater(): Boolean {
        sendMessage(HasAnnotationsFileUpdaterMessage()).await()
        return readMessage<Boolean>().await()
    }

    override suspend fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) {
        sendMessage(UpdateAnnotationsMessage(outdatedClassesJvmNames)).await()
    }

    override suspend fun revert() {
        sendMessage(RevertMessage()).await()
    }

    override suspend fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) {
        sendMessage(RegisterChangesMessage(timestamp, dirtyData)).await()
    }

    override suspend fun unknownChanges(timestamp: Long) {
        sendMessage(UnknownChangesMessage(timestamp)).await()
    }

    override suspend fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? {
        sendMessage(HasAnnotationsFileUpdaterMessage()).await()
        return readMessage<Iterable<SimpleDirtyData>?>().await()
    }

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        sendMessage(CompilerServicesFacadeBaseServerSide.ReportMessage(category, severity, message, attachment))
    }

}
