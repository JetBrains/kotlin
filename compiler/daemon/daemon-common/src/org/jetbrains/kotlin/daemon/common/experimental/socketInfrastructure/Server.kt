package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.actor
import java.io.Serializable
import java.net.InetSocketAddress

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
interface ServerBase

interface Server<out T : ServerBase> : ServerBase {

    enum class State {
        WORKING, CLOSED, ERROR, DOWNING
    }

    suspend fun processMessage(msg: AnyMessage<in T>, output: ByteWriteChannelWrapper): State

    suspend fun attachClient(client: Socket): Deferred<State>

    interface AnyMessage<ServerType : ServerBase> : Serializable

    interface Message<ServerType : ServerBase> : AnyMessage<ServerType> {
        suspend fun process(server: ServerType, output: ByteWriteChannelWrapper)
    }

    class EndConnectionMessage<ServerType : ServerBase> : AnyMessage<ServerType>

    class ServerDownMessage<ServerType : ServerBase> : AnyMessage<ServerType>

    fun runServer(): Deferred<Unit>

}

@Suppress("UNCHECKED_CAST")
class DefaultServer<out ServerType : ServerBase>(val serverPort: Int, val self: ServerType) : Server<ServerType> {

    final override suspend fun processMessage(msg: Server.AnyMessage<in ServerType>, output: ByteWriteChannelWrapper) = when (msg) {
        is Server.Message<in ServerType> -> Server.State.WORKING.also { msg.process(self as ServerType, output) }
        is Server.EndConnectionMessage<in ServerType> -> Server.State.CLOSED
        is Server.ServerDownMessage<in ServerType> -> Server.State.DOWNING
        else -> Server.State.ERROR
    }

    final override suspend fun attachClient(client: Socket) = async {
        val (input, output) = client.openIO()
        var finalState = Server.State.WORKING
        loop@
        while (true) {
            val state = processMessage(input.nextObject() as Server.AnyMessage<ServerType>, output)
            when (state) {
                Server.State.WORKING -> continue@loop
                else -> {
                    finalState = state
                    break@loop
                }
            }
        }
        finalState
    }

    final override fun runServer(): Deferred<Unit> {
        Report.log("binding to address($serverPort)", "DefaultSetver")
        return aSocket().tcp().bind(InetSocketAddress(serverPort)).use { serverSocket ->
            async {
                Report.log("accepting clientSocket...", "DefaultSetver")
                var shouldBreak = false
                while (true) {
                    if (shouldBreak) {
                        break
                    }
                    val client = serverSocket.accept()
                    Report.log("client accepted! (${client.remoteAddress})", "DefaultServer")
                    attachClient(client).invokeOnCompletion {
                        when (it) {
                            Server.State.DOWNING -> {
                                shouldBreak = true
                            }
                            else -> {
                            }
                        }
                    }
                    val cor = async { }
                }
            }
        }
    }

    protected fun finalize() {
        println("___________________\n[DEFAULT_SERVER] : FINALIZE (DOWNING!!!!!!!!)\n___________________")
    }

}