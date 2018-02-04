/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Client
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import java.rmi.Remote
import java.rmi.RemoteException


interface RemoteOutputStream : Remote {

    @Throws(RemoteException::class)
    fun close()

    @Throws(RemoteException::class)
    fun write(data: ByteArray, offset: Int, length: Int)

    @Throws(RemoteException::class)
    fun write(dataByte: Int)
}

interface RemoteOutputStreamClientSide : RemoteOutputStream, Client

interface RemoteOutputStreamServerSide : RemoteOutputStream, Server {
    // Query messages:
    class CloseMessage : Message<RemoteOutputStreamServerSide> {
        suspend override fun process(server: RemoteOutputStreamServerSide, output: ByteWriteChannelWrapper) =
            server.close()
    }

    class WriteMessage(val data: ByteArray, val offset: Int = -1, val length: Int = -1) : Message<RemoteOutputStreamServerSide> {
        suspend override fun process(server: RemoteOutputStreamServerSide, output: ByteWriteChannelWrapper) =
            server.write(data, offset, length)
    }

    class WriteIntMessage(val dataByte: Int) : Message<RemoteOutputStreamServerSide> {
        suspend override fun process(server: RemoteOutputStreamServerSide, output: ByteWriteChannelWrapper) =
            server.write(dataByte)
    }
}

