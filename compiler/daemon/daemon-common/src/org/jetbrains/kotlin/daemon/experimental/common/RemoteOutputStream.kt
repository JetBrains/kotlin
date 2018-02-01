/*
 * Copyright 2010-2015 JetBrains s.r.o.
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


interface RemoteOutputStream : Server, Remote {

    @Throws(RemoteException::class)
    fun close()

    @Throws(RemoteException::class)
    fun write(data: ByteArray, offset: Int, length: Int)

    @Throws(RemoteException::class)
    fun write(dataByte: Int)

    // Query messages:
    class CloseMessage : Message<RemoteOutputStream> {
        suspend override fun process(server: RemoteOutputStream, clientSocket: Socket) =
            server.close()
    }

    class WriteMessage(val data: ByteArray, val offset: Int = -1, val length: Int = -1) : Message<RemoteOutputStream> {
        suspend override fun process(server: RemoteOutputStream, clientSocket: Socket) =
            if (offset == -1)
                server.write(data[0])
            else
                server.write(data, offset, length)
    }
}
