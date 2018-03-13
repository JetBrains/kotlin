/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server


interface RemoteOutputStreamAsyncServerSide : RemoteOutputStreamAsync, Server<RemoteOutputStreamAsyncServerSide> {
    // Query messages:
    class CloseMessage : Server.Message<RemoteOutputStreamAsyncServerSide> {
        override suspend fun process(server: RemoteOutputStreamAsyncServerSide, output: ByteWriteChannelWrapper) =
            server.closeStream()
    }

    class WriteMessage(val data: ByteArray, val offset: Int = -1, val length: Int = -1) :
        Server.Message<RemoteOutputStreamAsyncServerSide> {
        override suspend fun process(server: RemoteOutputStreamAsyncServerSide, output: ByteWriteChannelWrapper) =
            server.write(data, offset, length)
    }

    class WriteIntMessage(val dataByte: Int) : Server.Message<RemoteOutputStreamAsyncServerSide> {
        override suspend fun process(server: RemoteOutputStreamAsyncServerSide, output: ByteWriteChannelWrapper) =
            server.write(dataByte)
    }
}


interface RemoteInputStreamServerSide : RemoteInputStreamAsync, Server<RemoteInputStreamServerSide> {
    // Query messages:
    class CloseMessage : Server.Message<RemoteInputStreamServerSide> {
        override suspend fun process(server: RemoteInputStreamServerSide, output: ByteWriteChannelWrapper) =
            server.closeStream()
    }

    class ReadMessage(val length: Int = -1) : Server.Message<RemoteInputStreamServerSide> {
        override suspend fun process(server: RemoteInputStreamServerSide, output: ByteWriteChannelWrapper) =
            output.writeObject(if (length == -1) server.read() else server.read(length))
    }
}