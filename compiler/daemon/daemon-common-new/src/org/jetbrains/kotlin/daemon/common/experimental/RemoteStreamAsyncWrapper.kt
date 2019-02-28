/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.RemoteInputStream
import org.jetbrains.kotlin.daemon.common.RemoteOutputStream
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.DefaultClientRMIWrapper

class RemoteOutputStreamAsyncWrapper(val rmiOutput: RemoteOutputStream) : RemoteOutputStreamAsyncClientSide,
    Client<RemoteOutputStreamAsyncServerSide> by DefaultClientRMIWrapper() {

    override suspend fun closeStream() =
        rmiOutput.close()

    override suspend fun write(data: ByteArray, offset: Int, length: Int) =
        rmiOutput.write(data, offset, length)

    override suspend fun write(dataByte: Int) =
        rmiOutput.write(dataByte)

}

class RemoteInputStreamAsyncWrapper(private val rmiInput: RemoteInputStream) : RemoteInputStreamClientSide,
    Client<RemoteInputStreamServerSide> by DefaultClientRMIWrapper() {

    override suspend fun closeStream() =
        rmiInput.close()

    override suspend fun read() =
        rmiInput.read()

    override suspend fun read(length: Int) =
        rmiInput.read(length)
}

fun RemoteOutputStream.toClient() = RemoteOutputStreamAsyncWrapper(this)
fun RemoteInputStream.toClient() = RemoteInputStreamAsyncWrapper(this)