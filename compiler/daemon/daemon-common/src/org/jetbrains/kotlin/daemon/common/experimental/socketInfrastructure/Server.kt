package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.jetbrains.kotlin.daemon.common.experimental.AUTH_TIMEOUT_IN_MILLISECONDS
import org.jetbrains.kotlin.daemon.common.experimental.FIRST_HANDSHAKE_BYTE_TOKEN
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterface
import java.io.Serializable
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

interface ServerBase

@Suppress("UNCHECKED_CAST")
interface Server<out T : ServerBase> : ServerBase {

    val serverPort: Int

    private val log: Logger
        get() = Logger.getLogger("default server")

    enum class State {
        WORKING, CLOSED, ERROR, DOWNING
    }

    suspend fun processMessage(msg: AnyMessage<in T>, output: ByteWriteChannelWrapper): State = when (msg) {
        is Server.Message<in T> -> Server.State.WORKING.also { msg.process(this as T, output) }
        is Server.EndConnectionMessage<in T> -> {
            println("!EndConnectionMessage!")
            Server.State.CLOSED
        }
        is Server.ServerDownMessage<in T> -> Server.State.DOWNING
        else -> Server.State.ERROR
    }

    suspend fun attachClient(client: Socket): Deferred<State> = async {
        val (input, output) = client.openIO(log)
        try {
            tryAcquireHandshakeMessage(input, log)
            sendHandshakeMessage(output)
        } catch (e: Throwable) {
            log.info("NO TOKEN")
            return@async Server.State.CLOSED
        }
        var finalState = Server.State.WORKING
        loop@
        while (true) {
            val state = processMessage(input.nextObject() as Server.AnyMessage<T>, output)
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

    interface AnyMessage<ServerType : ServerBase> : Serializable

    interface Message<ServerType : ServerBase> : AnyMessage<ServerType> {
        suspend fun process(server: ServerType, output: ByteWriteChannelWrapper)
    }

    class EndConnectionMessage<ServerType : ServerBase> : AnyMessage<ServerType>

    class ServerDownMessage<ServerType : ServerBase> : AnyMessage<ServerType>

    fun runServer(): Deferred<Unit> {
        log.info("binding to address($serverPort)")
        val serverSocket = LoopbackNetworkInterface.serverLoopbackSocketFactoryKtor.createServerSocket(
            serverPort
        )
        return async {
            serverSocket.use {
                log.info("accepting clientSocket...")
                while (true) {
                    val client = serverSocket.accept()
                    log.info("client accepted! (${client.remoteAddress})")
                    attachClient(client).invokeOnCompletion {
                        when (it) {
                            Server.State.DOWNING -> {
                                client.close()
                            }
                            else -> {
                                client.close()
                            }
                        }
                    }
                }
            }
        }
    }

}

@Throws(Exception::class)
suspend fun tryAcquireHandshakeMessage(input: ByteReadChannelWrapper, log: Logger): Boolean {
    val bytesAsync = async { input.readBytes(FIRST_HANDSHAKE_BYTE_TOKEN.size) }
    delay(AUTH_TIMEOUT_IN_MILLISECONDS, TimeUnit.MILLISECONDS)
    val bytes = bytesAsync.getCompleted()
    log.info("bytes : ${bytes.toList()}")
    if (bytes.zip(FIRST_HANDSHAKE_BYTE_TOKEN).any { it.first != it.second }) {
        log.info("BAD TOKEN")
        return false
    }
    return true
}

suspend fun sendHandshakeMessage(output: ByteWriteChannelWrapper) {
    output.printBytes(FIRST_HANDSHAKE_BYTE_TOKEN)
}