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


public val SOCKET_ANY_FREE_PORT  = 0

public object LoopbackNetworkInterface {

    val IPV4_LOOPBACK_INET_ADDRESS = "127.0.0.1"
    val IPV6_LOOPBACK_INET_ADDRESS = "::1"

    val SERVER_SOCKET_BACKLOG_SIZE = 5 // size of the requests queue for daemon services, so far seems that we don't need any big numbers here
                                       // but if we'll start getting "connection refused" errors, that could be the first place to try to fix it

    public val serverLoopbackSocketFactory by lazy { ServerLoopbackSocketFactory() }
    public val clientLoopbackSocketFactory by lazy { ClientLoopbackSocketFactory() }

    // TODO switch to InetAddress.getLoopbackAddress on java 7+
    public val loopbackInetAddressName by lazy {
        try {
            if (java.net.InetAddress.getLocalHost() is java.net.Inet6Address) IPV6_LOOPBACK_INET_ADDRESS else IPV4_LOOPBACK_INET_ADDRESS
        }
        catch (e: IOException) {
            // getLocalHost may fail for unknown reasons in some situations, the fallback is to assume IPv4 for now
            // TODO consider some other ways to detect default to IPv6 addresses in this case
            IPV4_LOOPBACK_INET_ADDRESS
        }
    }

    // base socket factories by default don't implement equals properly (see e.g. http://stackoverflow.com/questions/21555710/rmi-and-jmx-socket-factories)
    // so implementing it in derived classes using the fact that they are singletons

    class ServerLoopbackSocketFactory : RMIServerSocketFactory, Serializable {
        override fun equals(other: Any?): Boolean = other === this || super.equals(other)

        @Throws(IOException::class)
        override fun createServerSocket(port: Int): ServerSocket = ServerSocket(port, SERVER_SOCKET_BACKLOG_SIZE, InetAddress.getByName(loopbackInetAddressName))
    }


    class ClientLoopbackSocketFactory : RMIClientSocketFactory, Serializable {
        override fun equals(other: Any?): Boolean = other === this || super.equals(other)

        @Throws(IOException::class)
        override fun createSocket(host: String, port: Int): Socket = Socket(InetAddress.getByName(loopbackInetAddressName), port)
    }
}



