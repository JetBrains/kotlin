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


object LoopbackNetworkInterfaceKtor {

    val serverLoopbackSocketFactoryKtor by lazy { ServerLoopbackSocketFactoryKtor() }
    val clientLoopbackSocketFactoryKtor by lazy { ClientLoopbackSocketFactoryKtor() }

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

    class ClientLoopbackSocketFactoryKtor : LoopbackNetworkInterface.AbstractClientLoopbackSocketFactory<io.ktor.network.sockets.Socket>() {
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
                LoopbackNetworkInterfaceKtor.serverLoopbackSocketFactoryKtor.createServerSocket(port)
            )
        } catch (e: Exception) {
            // assuming that the socketPort is already taken
            lastException = e
        }
    }
    throw IllegalStateException("Cannot find free socketPort in $attempts attempts", lastException)
}