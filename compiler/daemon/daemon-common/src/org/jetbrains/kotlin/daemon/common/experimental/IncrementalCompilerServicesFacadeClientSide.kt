/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.async
import org.jetbrains.kotlin.daemon.common.SimpleDirtyData
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteReadChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openAndWrapReadChannel
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openAndWrapWriteChannel
import java.beans.Transient
import java.io.File
import java.io.Serializable
import java.net.InetSocketAddress

interface IncrementalCompilerServicesFacadeClientSide : IncrementalCompilerServicesFacadeAsync, CompilerServicesFacadeBaseClientSide

class IncrementalCompilerServicesFacadeClientSideImpl(val serverPort: Int) : IncrementalCompilerServicesFacadeClientSide {

    @Transient
    lateinit var input: ByteReadChannelWrapper
    @Transient
    lateinit var output: ByteWriteChannelWrapper

    override suspend fun hasAnnotationsFileUpdater(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun revert() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun unknownChanges(timestamp: Long) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun connectToServer() {
        async {
            aSocket().tcp().connect(InetSocketAddress(serverPort)).let {
                input = it.openAndWrapReadChannel()
                output = it.openAndWrapWriteChannel()
            }
        }
    }

}
