package org.jetbrains.kotlin.daemon.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import java.io.Serializable

interface Server {

    suspend fun processMessage(msg: Any, client: Socket)

    suspend fun send(client: Socket, msg: Any?)

    fun attachClient(client: Socket)

    fun detachClient(client: Socket)

    interface Message<in ServerType : Server> : Serializable {
        suspend fun process(server: ServerType, clientSocket: Socket)
    }

}