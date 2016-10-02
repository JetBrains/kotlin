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

package org.jetbrains.kotlin.daemon.common

import java.io.IOException
import java.io.Serializable
import java.net.Inet6Address
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.RMIClientSocketFactory
import java.rmi.server.RMIServerSocketFactory
import java.util.*


val SOCKET_ANY_FREE_PORT  = 0

object LoopbackNetworkInterface {

    val IPV4_LOOPBACK_INET_ADDRESS = "127.0.0.1"
    val IPV6_LOOPBACK_INET_ADDRESS = "::1"

    val SERVER_SOCKET_BACKLOG_SIZE = 10 // size of the requests queue for daemon services, so far seems that we don't need any big numbers here
                                        // but if we'll start getting "connection refused" errors, that could be the first place to try to fix it

    val serverLoopbackSocketFactory by lazy { ServerLoopbackSocketFactory() }
    val clientLoopbackSocketFactory by lazy { ClientLoopbackSocketFactory() }

    // TODO switch to InetAddress.getLoopbackAddress on java 7+
    val loopbackInetAddressName by lazy {
        try {
            if (InetAddress.getByName(null) is Inet6Address) IPV6_LOOPBACK_INET_ADDRESS else IPV4_LOOPBACK_INET_ADDRESS
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
        override fun hashCode(): Int = super.hashCode()

        @Throws(IOException::class)
        override fun createServerSocket(port: Int): ServerSocket = ServerSocket(port, SERVER_SOCKET_BACKLOG_SIZE, InetAddress.getByName(null))
    }


    class ClientLoopbackSocketFactory : RMIClientSocketFactory, Serializable {
        override fun equals(other: Any?): Boolean = other === this || super.equals(other)
        override fun hashCode(): Int = super.hashCode()

        @Throws(IOException::class)
        override fun createSocket(host: String, port: Int): Socket = Socket(InetAddress.getByName(null), port)
    }
}


private val portSelectionRng = Random()

fun findPortAndCreateRegistry(attempts: Int, portRangeStart: Int, portRangeEnd: Int) : Pair<Registry, Int> {
    var i = 0
    var lastException: RemoteException? = null

    while (i++ < attempts) {
        val port = portSelectionRng.nextInt(portRangeEnd - portRangeStart) + portRangeStart
        try {
            return Pair(LocateRegistry.createRegistry(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory), port)
        }
        catch (e: RemoteException) {
            // assuming that the port is already taken
            lastException = e
        }
    }
    throw IllegalStateException("Cannot find free port in $attempts attempts", lastException)
}

