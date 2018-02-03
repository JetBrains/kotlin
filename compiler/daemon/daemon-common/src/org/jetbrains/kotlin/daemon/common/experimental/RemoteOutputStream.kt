/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.Message
import java.rmi.Remote
import java.rmi.RemoteException


interface RemoteOutputStream : Server, Remote {

    @Throws(RemoteException::class)
    fun close()

    @Throws(RemoteException::class)
    fun write(data: ByteArray, offset: Int, length: Int)

    @Throws(RemoteException::class)
    fun write(dataByte: Int)

    // Query messages:
    class CloseMessage : Message<RemoteOutputStream> {
        suspend override fun process(server: RemoteOutputStream, output: ByteWriteChannelWrapper) =
            server.close()
    }

    class WriteMessage(val data: ByteArray, val offset: Int = -1, val length: Int = -1) : Message<RemoteOutputStream> {
        suspend override fun process(server: RemoteOutputStream, output: ByteWriteChannelWrapper) =
            server.write(data, offset, length)
    }

    class WriteIntMessage(val dataByte: Int) : Message<RemoteOutputStream> {
        suspend override fun process(server: RemoteOutputStream, output: ByteWriteChannelWrapper) =
            server.write(dataByte)
    }

}
