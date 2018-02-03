package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import java.io.Serializable

interface Server {

    enum class State {
        WORKING, CLOSED, ERROR
    }

    suspend fun processMessage(msg: AnyMessage, output: ByteWriteChannelWrapper): State

    suspend fun attachClient(client: Socket)

    interface AnyMessage: Serializable

    interface Message<ServerType : Server> : AnyMessage {
        suspend fun process(server: ServerType, output: ByteWriteChannelWrapper)
    }

    class EndConnectionMessage: AnyMessage

}