/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.impls

import java.io.IOException
import java.io.Serializable
import java.net.*
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.RMIClientSocketFactory
import java.rmi.server.RMIServerSocketFactory
import java.security.*
import java.util.*


const val SOCKET_ANY_FREE_PORT  = 0
const val JAVA_RMI_SERVER_HOSTNAME = "java.rmi.server.hostname"
const val DAEMON_RMI_SOCKET_BACKLOG_SIZE_PROPERTY = "kotlin.daemon.socket.backlog.size"
const val DAEMON_RMI_SOCKET_CONNECT_ATTEMPTS_PROPERTY = "kotlin.daemon.socket.connect.attempts"
const val DAEMON_RMI_SOCKET_CONNECT_INTERVAL_PROPERTY = "kotlin.daemon.socket.connect.interval"
const val DEFAULT_SERVER_SOCKET_BACKLOG_SIZE = 50
const val DEFAULT_SOCKET_CONNECT_ATTEMPTS = 3
const val DEFAULT_SOCKET_CONNECT_INTERVAL_MS = 10L

object LoopbackNetworkInterface {


    init {
         val keyGen = KeyPairGenerator.getInstance("DSA",
            "SUN");
        val random = SecureRandom.getInstance("SHA1PRNG",
                                              "SUN");

        keyGen.initialize(1024, random);

        val pair = keyGen.generateKeyPair();
        val priv = pair.getPrivate();
        val pub = pair.getPublic();

        /*
         * Create a Signature object and initialize it with the private
         * key
         */

        val dsa = Signature.getInstance("SHA1withDSA", "SUN");

        dsa.toString()

        dsa.initSign(priv);

    }

    const val IPV4_LOOPBACK_INET_ADDRESS = "127.0.0.1"
    const val IPV6_LOOPBACK_INET_ADDRESS = "::1"

    // size of the requests queue for daemon services, so far seems that we don't need any big numbers here
    // but if we'll start getting "connection refused" errors, that could be the first place to try to fix it
    val SERVER_SOCKET_BACKLOG_SIZE by lazy { System.getProperty(DAEMON_RMI_SOCKET_BACKLOG_SIZE_PROPERTY)?.toIntOrNull() ?: DEFAULT_SERVER_SOCKET_BACKLOG_SIZE }
    val SOCKET_CONNECT_ATTEMPTS by lazy { System.getProperty(DAEMON_RMI_SOCKET_CONNECT_ATTEMPTS_PROPERTY)?.toIntOrNull() ?: DEFAULT_SOCKET_CONNECT_ATTEMPTS }
    val SOCKET_CONNECT_INTERVAL_MS by lazy { System.getProperty(DAEMON_RMI_SOCKET_CONNECT_INTERVAL_PROPERTY)?.toLongOrNull() ?: DEFAULT_SOCKET_CONNECT_INTERVAL_MS }

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
        override fun createServerSocket(port: Int): ServerSocket = ServerSocket(port,
                                                                                SERVER_SOCKET_BACKLOG_SIZE, InetAddress.getByName(null))
    }


    class ClientLoopbackSocketFactory : RMIClientSocketFactory, Serializable {
        override fun equals(other: Any?): Boolean = other === this || super.equals(other)
        override fun hashCode(): Int = super.hashCode()

        @Throws(IOException::class)
        override fun createSocket(host: String, port: Int): Socket {
            var attemptsLeft = SOCKET_CONNECT_ATTEMPTS
            while (true) {
                try {
                    return Socket(InetAddress.getByName(null), port)
                }
                catch (e: ConnectException) {
                    if (--attemptsLeft <= 0) throw e
                }
                Thread.sleep(SOCKET_CONNECT_INTERVAL_MS)
            }
        }
    }
}


private val portSelectionRng = Random()

fun findPortAndCreateRegistry(attempts: Int, portRangeStart: Int, portRangeEnd: Int) : Pair<Registry, Int> {
    var i = 0
    var lastException: RemoteException? = null

    while (i++ < attempts) {
        val port = portSelectionRng.nextInt(portRangeEnd - portRangeStart) + portRangeStart
        try {
            return Pair(LocateRegistry.createRegistry(port,
                                                      LoopbackNetworkInterface.clientLoopbackSocketFactory,
                                                      LoopbackNetworkInterface.serverLoopbackSocketFactory
            ), port)
        }
        catch (e: RemoteException) {
            // assuming that the socketPort is already taken
            lastException = e
        }
    }
    throw IllegalStateException("Cannot find free socketPort in $attempts attempts", lastException)
}

/**
 * Needs to be set up on both client and server to prevent localhost resolution,
 * which may be slow and can cause a timeout when there is a network problem/misconfiguration.
 */
fun ensureServerHostnameIsSetUp() {
    if (System.getProperty(JAVA_RMI_SERVER_HOSTNAME) == null) {
        System.setProperty(
            JAVA_RMI_SERVER_HOSTNAME,
            LoopbackNetworkInterface.loopbackInetAddressName
        )
    }
}
