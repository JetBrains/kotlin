package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket


interface Client {
    fun attachToServer(socket: Socket)
//    fun ask(server: ServerType, query: Message<ServerType>): Any
//    fun tell(server: ServerType, query: Message<ServerType>)
}