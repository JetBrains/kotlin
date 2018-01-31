package org.jetbrains.kotlin.daemon.experimental.socketInfrastructure

import org.jetbrains.kotlin.daemon.experimental.socketInfrastructure.Server.Message

interface Client<ServerType: Server> {
    fun ask(server: ServerType, query: Message<ServerType>): Any
    fun tell(server: ServerType, query: Message<ServerType>)
}