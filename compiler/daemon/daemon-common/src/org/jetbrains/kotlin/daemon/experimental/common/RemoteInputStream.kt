/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.experimental.common

import java.rmi.Remote
import java.rmi.RemoteException
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server
import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server.Message
import io.ktor.network.sockets.Socket

interface RemoteInputStream : Server, Remote {

    @Throws(RemoteException::class)
    fun close()

    @Throws(RemoteException::class)
    fun read(length: Int): ByteArray

    @Throws(RemoteException::class)
    fun read(): Int

    // Query messages:
    class CloseMessage : Message<RemoteInputStream> {
        suspend override fun process(server: RemoteInputStream, clientSocket: Socket) =
            server.close()
    }

    class ReadMessage(val length: Int = -1) : Message<RemoteInputStream> {
        suspend override fun process(server: RemoteInputStream, clientSocket: Socket) =
            server.send(
                clientSocket,
                if (length == -1) server.read() else server.read(length)
            )
    }

}
