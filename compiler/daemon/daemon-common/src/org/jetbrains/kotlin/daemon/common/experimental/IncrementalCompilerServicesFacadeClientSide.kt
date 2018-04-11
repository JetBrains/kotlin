/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.future.await
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
        val id = sendMessage(HasAnnotationsFileUpdaterMessage())
        return readMessage(id)
    }

    override suspend fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) {
        val id = sendMessage(UpdateAnnotationsMessage(outdatedClassesJvmNames))
    }

    override suspend fun revert() {
        val id = sendMessage(RevertMessage())
    }

    override suspend fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) {
        val id = sendMessage(RegisterChangesMessage(timestamp, dirtyData))
    }

    override suspend fun unknownChanges(timestamp: Long) {
        val id = sendMessage(UnknownChangesMessage(timestamp))
    }

    override suspend fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? {
        val id = sendMessage(HasAnnotationsFileUpdaterMessage())
        return readMessage(id)
    }

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        val id = sendMessage(CompilerServicesFacadeBaseServerSide.ReportMessage(category, severity, message, attachment))
    }

}
