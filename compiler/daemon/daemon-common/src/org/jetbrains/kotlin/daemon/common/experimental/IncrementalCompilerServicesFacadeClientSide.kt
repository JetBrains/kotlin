/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import io.ktor.network.sockets.Socket
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteReadChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openAndWrapReadChannel
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openAndWrapWriteChannel
import java.io.File

class IncrementalCompilerServicesFacadeClientSide: IncrementalCompilerServicesFacadeAsync, CompilerServicesFacadeBaseClientSide {

    lateinit var socketToServer: Socket
    val input: ByteReadChannelWrapper by lazy { socketToServer.openAndWrapReadChannel() }
    val output: ByteWriteChannelWrapper by lazy { socketToServer.openAndWrapWriteChannel() }

    suspend override fun hasAnnotationsFileUpdater(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun revert() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun unknownChanges(timestamp: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }



    override fun attachToServer(socket: Socket) {
        socketToServer = socket
    }

    init {
        attachToServer(socketFactory.createSocket(host, port))
    }

}
