package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.ByteWriteChannelWrapper
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.openIO
import java.io.Serializable
import java.net.InetSocketAddress
/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
interface ServerBase

interface Server<T : ServerBase> : ServerBase {

    enum class State {
        WORKING, CLOSED, ERROR
    }

    suspend fun processMessage(msg: AnyMessage<T>, output: ByteWriteChannelWrapper): State

    suspend fun attachClient(client: Socket)

    interface AnyMessage<ServerType : ServerBase> : Serializable

    interface Message<ServerType : ServerBase> : AnyMessage<ServerType> {
        suspend fun process(server: ServerType, output: ByteWriteChannelWrapper)
    }

    class EndConnectionMessage<ServerType : ServerBase> : AnyMessage<ServerType>

    fun runServer()

}

@Suppress("UNCHECKED_CAST")
class DefaultServer<ServerType : ServerBase> (val serverPort: Int) : Server<ServerType> {

    final override suspend fun processMessage(msg: Server.AnyMessage<ServerType>, output: ByteWriteChannelWrapper) = when(msg) {
        is Server.Message<ServerType> -> Server.State.WORKING.also { msg.process(this as ServerType, output) }
        is Server.EndConnectionMessage<ServerType> -> Server.State.CLOSED
        else -> Server.State.ERROR
    }

    final override suspend fun attachClient(client: Socket) {
        async {
            val (input, output) = client.openIO()
            loop@ while (true) {
                when (processMessage(input.nextObject() as Server.AnyMessage<ServerType>, output)) {
                    Server.State.CLOSED -> break@loop
                    Server.State.ERROR -> {
                        // TODO: debug message "Server error: invalid message"
                    }
                }
            }
        }
    }

    final override fun runServer() {
        runBlocking {
            aSocket().tcp().bind(InetSocketAddress(serverPort)).use { serverSocket ->
                println("accepting clientSocket...")
                while (true) {
                    val client = serverSocket.accept()
                    println("client accepted! (${client.remoteAddress})")
                    attachClient(client)
                }
            }
        }
    }

}