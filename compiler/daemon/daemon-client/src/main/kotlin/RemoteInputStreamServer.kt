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

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.RemoteInputStream
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import java.io.InputStream
import java.rmi.server.UnicastRemoteObject


class RemoteInputStreamServer(val `in`: InputStream, port: Int = SOCKET_ANY_FREE_PORT)
: RemoteInputStream,
  UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
{
    override fun close() {
        `in`.close()
    }

    override fun read(length: Int): ByteArray {
        val buf = ByteArray(length)
        val readBytes = `in`.read(buf, 0, length)
        return if (readBytes == length) buf
               else buf.copyOfRange(0, readBytes)
    }

    override fun read(): Int =
            `in`.read()
}
