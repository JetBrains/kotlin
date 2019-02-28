/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.*
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ServerSocketWrapper
import org.jetbrains.kotlin.daemon.common.*
import java.io.IOException
import java.io.Serializable
import java.net.*
import java.rmi.server.RMIClientSocketFactory
import java.rmi.server.RMIServerSocketFactory
import java.util.*


// copyed from original(org.jetbrains.kotlin.daemon.common.NetworkUtils) TODO
// unique part :
// - AbstractClientLoopbackSocketFactory / ServerLoopbackSocketFactoryRMI / ServerLoopbackSocketFactoryKtor - Ktor-Sockets instead of java sockets
// - findPortAndCreateSocket
// TODO: get rid of copy-paste here

object LoopbackNetworkInterface {

    const val IPV4_LOOPBACK_INET_ADDRESS = "127.0.0.1"
    const val IPV6_LOOPBACK_INET_ADDRESS = "::1"

    // size of the requests queue for daemon services, so far seems that we don't need any big numbers here
    // but if we'll start getting "connection refused" errors, that could be the first place to try to fix it
    val SERVER_SOCKET_BACKLOG_SIZE by lazy {
        System.getProperty(DAEMON_RMI_SOCKET_BACKLOG_SIZE_PROPERTY)?.toIntOrNull() ?: DEFAULT_SERVER_SOCKET_BACKLOG_SIZE
    }
    val SOCKET_CONNECT_ATTEMPTS by lazy {
        System.getProperty(DAEMON_RMI_SOCKET_CONNECT_ATTEMPTS_PROPERTY)?.toIntOrNull() ?: DEFAULT_SOCKET_CONNECT_ATTEMPTS
    }
    val SOCKET_CONNECT_INTERVAL_MS by lazy {
        System.getProperty(DAEMON_RMI_SOCKET_CONNECT_INTERVAL_PROPERTY)?.toLongOrNull() ?: DEFAULT_SOCKET_CONNECT_INTERVAL_MS
    }

    val serverLoopbackSocketFactoryRMI by lazy { ServerLoopbackSocketFactoryRMI() }
    val clientLoopbackSocketFactoryRMI by lazy { ClientLoopbackSocketFactoryRMI() }

    val serverLoopbackSocketFactoryKtor by lazy { ServerLoopbackSocketFactoryKtor() }
    val clientLoopbackSocketFactoryKtor by lazy { ClientLoopbackSocketFactoryKtor() }

    // TODO switch to InetAddress.getLoopbackAddress on java 7+
    val loopbackInetAddressName by lazy {
        try {
            if (InetAddress.getByName(null) is Inet6Address) IPV6_LOOPBACK_INET_ADDRESS else IPV4_LOOPBACK_INET_ADDRESS
        } catch (e: IOException) {
            // getLocalHost may fail for unknown reasons in some situations, the fallback is to assume IPv4 for now
            // TODO consider some other ways to detect default to IPv6 addresses in this case
            IPV4_LOOPBACK_INET_ADDRESS
        }
    }

    // base socket factories by default don't implement equals properly (see e.g. http://stackoverflow.com/questions/21555710/rmi-and-jmx-socket-factories)
    // so implementing it in derived classes using the fact that they are singletons

    class ServerLoopbackSocketFactoryRMI : RMIServerSocketFactory, Serializable {
        override fun equals(other: Any?): Boolean = other === this || super.equals(other)
        override fun hashCode(): Int = super.hashCode()

        @Throws(IOException::class)
        override fun createServerSocket(port: Int): java.net.ServerSocket =
            ServerSocket(port, SERVER_SOCKET_BACKLOG_SIZE, InetAddress.getByName(null))
    }

    val selectorMgr = ActorSelectorManager(Dispatchers.IO)

    class ServerLoopbackSocketFactoryKtor : Serializable {
        override fun equals(other: Any?): Boolean = other === this || super.equals(other)
        override fun hashCode(): Int = super.hashCode()

        @Throws(IOException::class)
        fun createServerSocket(port: Int) =
            aSocket(selectorMgr)
                .tcp()
                .bind(InetSocketAddress(InetAddress.getByName(null), port)) // TODO : NO BACKLOG SIZE CHANGE =(
    }

    abstract class AbstractClientLoopbackSocketFactory<SocketType> : Serializable {
        override fun equals(other: Any?): Boolean = other === this || super.equals(other)
        override fun hashCode(): Int = super.hashCode()

        abstract protected fun socketCreate(host: String, port: Int): SocketType

        @Throws(IOException::class)
        fun createSocket(host: String, port: Int): SocketType {
            var attemptsLeft = SOCKET_CONNECT_ATTEMPTS
            while (true) {
                try {
                    return socketCreate(host, port)
                } catch (e: ConnectException) {
                    if (--attemptsLeft <= 0) throw e
                }
                Thread.sleep(SOCKET_CONNECT_INTERVAL_MS)
            }
        }
    }

    class ClientLoopbackSocketFactoryRMI : AbstractClientLoopbackSocketFactory<java.net.Socket>(), RMIClientSocketFactory {
        override fun socketCreate(host: String, port: Int): Socket = Socket(InetAddress.getByName(null), port)
    }

    class ClientLoopbackSocketFactoryKtor : AbstractClientLoopbackSocketFactory<io.ktor.network.sockets.Socket>() {
        override fun socketCreate(host: String, port: Int): io.ktor.network.sockets.Socket =
            runBlocking { aSocket(selectorMgr).tcp().connect(InetSocketAddress(host, port)) }
    }

}


private val portSelectionRng = Random()

fun findPortForSocket(attempts: Int, portRangeStart: Int, portRangeEnd: Int): ServerSocketWrapper {
    var i = 0
    var lastException: Exception? = null

    while (i++ < attempts) {
        val port = portSelectionRng.nextInt(portRangeEnd - portRangeStart) + portRangeStart
        try {
            return ServerSocketWrapper(
                port,
                LoopbackNetworkInterface.serverLoopbackSocketFactoryKtor.createServerSocket(port)
            )
        } catch (e: Exception) {
            // assuming that the socketPort is already taken
            lastException = e
        }
    }
    throw IllegalStateException("Cannot find free socketPort in $attempts attempts", lastException)
}