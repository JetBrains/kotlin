package org.jetbrains.kotlin.daemon.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import java.io.Serializable

interface Server {

    enum class State {
        WORKING, CLOSED
    }

    suspend fun processMessage(msg: Message<*>, output: ByteWriteChannelWrapper): State

    suspend fun attachClient(client: Socket)

    interface Message<ServerType : Server> : Serializable {
        suspend fun process(server: ServerType, output: ByteWriteChannelWrapper)
    }

    interface EndConnectionMessage<ServerType : Server>: Message<ServerType>

    val END_CONNECTION_MESSAGE: EndConnectionMessage<*>
}