/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("EXPERIMENTAL_FEATURE_WARNING")

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.*
import java.io.File
import io.ktor.network.sockets.Socket
import java.io.Serializable
import org.jetbrains.kotlin.daemon.common.experimental.IncrementalCompilerServicesFacadeServerSide.*
import org.jetbrains.kotlin.daemon.common.experimental.CompilerServicesFacadeBaseServerSide.*

class IncrementalCompilerServicesFacadeClientSide(socketOfServer: Socket): IncrementalCompilerServicesFacadeAsync, CompilerServicesFacadeBaseClientSide {

    lateinit var socketToServer: Socket
    val input: ByteReadChannelWrapper by lazy { socketToServer.openAndWrapReadChannel() }
    val output: ByteWriteChannelWrapper by lazy { socketToServer.openAndWrapWriteChannel() }

    suspend override fun hasAnnotationsFileUpdater(): Boolean {
        output.writeObject(HasAnnotationsFileUpdaterMessage())
        return input.nextObject() as Boolean
    }

    suspend override fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) {
        output.writeObject(UpdateAnnotationsMessage(outdatedClassesJvmNames))
    }

    suspend override fun revert() {
        output.writeObject(RevertMessage())
    }

    suspend override fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) {
        output.writeObject(RegisterChangesMessage(timestamp, dirtyData))
    }

    suspend override fun unknownChanges(timestamp: Long) {
        output.writeObject(UnknownChangesMessage(timestamp))
    }

    suspend override fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? {
        output.writeObject(GetChangesMessage(artifact, sinceTS))
        return input.nextObject() as Iterable<SimpleDirtyData>?
    }

    suspend override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        output.writeObject(ReportMessage(category, severity, message, attachment))
    }

    override fun attachToServer(socket: Socket) {
        socketToServer = socket
    }

    init {
        attachToServer(socketOfServer)
    }

}
