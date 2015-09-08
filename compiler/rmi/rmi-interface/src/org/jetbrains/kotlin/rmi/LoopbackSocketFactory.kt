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

package org.jetbrains.kotlin.rmi

import java.io.IOException
import java.io.Serializable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.rmi.server.RMIClientSocketFactory
import java.rmi.server.RMIServerSocketFactory


// TODO switch to InetAddress.getLoopbackAddress on java 7+
val loopbackAddrName by lazy { if (java.net.InetAddress.getLocalHost() is java.net.Inet6Address) "::1" else "127.0.0.1" }
val loopbackAddr by lazy { InetAddress.getByName(loopbackAddrName) }
val serverLoopbackSocketFactory by lazy { ServerLoopbackSocketFactory() }
val clientLoopbackSocketFactory by lazy { ClientLoopbackSocketFactory() }


data class ServerLoopbackSocketFactory : RMIServerSocketFactory, Serializable {

    throws(IOException::class)
    override fun createServerSocket(port: Int): ServerSocket = ServerSocket(port, 5, loopbackAddr)
}


data class ClientLoopbackSocketFactory : RMIClientSocketFactory, Serializable {

    throws(IOException::class)
    override fun createSocket(host: String, port: Int): Socket = Socket(loopbackAddr, port)
}


