package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import kotlinx.coroutines.*
import org.jetbrains.kotlin.daemon.common.experimental.*
import java.io.Serializable
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

private fun Logger.info_and_print(msg: String) {
    this.info(msg)
//    println(msg)
}

data class ServerSocketWrapper(val port: Int, val socket: ServerSocket)

interface ServerBase

@Suppress("UNCHECKED_CAST")
interface Server<out T : ServerBase> : ServerBase {

    val serverSocketWithPort: ServerSocketWrapper
    val serverPort: Int
        get() = serverSocketWithPort.port

    private val log: Logger
        get() = Logger.getLogger("default server($serverPort)")

    enum class State {
        WORKING, CLOSED, ERROR, DOWNING, UNVERIFIED
    }

    fun processMessage(msg: AnyMessage<in T>, output: ByteWriteChannelWrapper): State =
        when (msg) {
            is Server.Message<in T> -> Server.State.WORKING.also {
                msg.process(this as T, output)
            }
            is Server.EndConnectionMessage<in T> -> {
                log.info_and_print("!EndConnectionMessage!")
                Server.State.CLOSED
            }
            is Server.ServerDownMessage<in T> -> Server.State.CLOSED
            else -> Server.State.ERROR
        }

    fun attachClient(client: Socket): Deferred<State> = async {
        val (input, output) = client.openIO(log)
        if (!serverHandshake(input, output, log)) {
            log.info_and_print("failed to establish connection with client (handshake failed)")
            return@async Server.State.UNVERIFIED
        }
        if (!securityCheck(input)) {
            log.info_and_print("failed to check securitay")
            return@async Server.State.UNVERIFIED
        }
        log.info_and_print("   client verified ($client)")
        clients[client] = ClientInfo(client, input, output)
        log.info_and_print("   ($client)client in clients($clients)")
        var finalState = Server.State.WORKING
        val keepAliveAcknowledgement = KeepAliveAcknowledgement<T>()
        loop@
        while (true) {
            log.info_and_print("   reading message from ($client)")
            val message = input.nextObject()
            when (message) {
                is Server.ServerDownMessage<*> -> {
                    downClient(client)
                    break@loop
                }
                is Server.KeepAliveMessage<*> -> Server.State.WORKING.also {
                    output.writeObject(
                        DefaultAuthorizableClient.MessageReply(
                            message.messageId!!,
                            keepAliveAcknowledgement
                        )
                    )
                }
                !is Server.AnyMessage<*> -> {
                    log.info_and_print("contrafact message")
                    finalState = Server.State.ERROR
                    break@loop
                }
                else -> {
                    log.info_and_print("message ($client): $message")
                    val state = processMessage(message as Server.AnyMessage<T>, output)
                    when (state) {
                        Server.State.WORKING -> continue@loop
                        Server.State.ERROR -> {
                            log.info_and_print("ERROR after processing message")
                            finalState = Server.State.ERROR
                            break@loop
                        }
                        else -> {
                            finalState = state
                            break@loop
                        }
                    }
                }
            }
        }
        finalState
    }

    abstract class AnyMessage<ServerType : ServerBase> : Serializable {
        var messageId: Int? = null
        fun withId(id: Int): AnyMessage<ServerType> {
            messageId = id
            return this
        }
    }

    abstract class Message<ServerType : ServerBase> : AnyMessage<ServerType>() {
        fun process(server: ServerType, output: ByteWriteChannelWrapper) = async {
            log.info("$server starts processing ${this@Message}")
            processImpl(server, {
                log.info("$server finished processing ${this@Message}, sending output")
                async {
                    log.info("$server starts sending ${this@Message} to output")
                    output.writeObject(DefaultAuthorizableClient.MessageReply(messageId ?: -1, it))
                    log.info("$server finished sending ${this@Message} to output")
                }
            })
        }

        abstract suspend fun processImpl(server: ServerType, sendReply: (Any?) -> Unit)
    }

    class EndConnectionMessage<ServerType : ServerBase> : AnyMessage<ServerType>()

    class KeepAliveAcknowledgement<ServerType : ServerBase> : AnyMessage<ServerType>()

    class KeepAliveMessage<ServerType : ServerBase> : AnyMessage<ServerType>()

    class ServerDownMessage<ServerType : ServerBase> : AnyMessage<ServerType>()

    data class ClientInfo(val socket: Socket, val input: ByteReadChannelWrapper, val output: ByteWriteChannelWrapper)

    val clients: HashMap<Socket, ClientInfo>

    fun runServer(): Deferred<Unit> {
        log.info_and_print("binding to address(${serverSocketWithPort.port})")
        val serverSocket = serverSocketWithPort.socket
        return async {
            serverSocket.use {
                log.info_and_print("accepting clientSocket...")
                while (true) {
                    val client = serverSocket.accept()
                    log.info_and_print("client accepted! (${client.remoteAddress})")
                    async {
                        val state = attachClient(client).await()
                        log.info_and_print("finished ($client) with state : $state")
                        when (state) {
                            Server.State.CLOSED, State.UNVERIFIED -> {
                                downClient(client)
                            }
                            Server.State.DOWNING -> {
                                downServer()
                            }
                            else -> {
                                downClient(client)
                            }
                        }
                    }
                }
            }
        }
    }

    fun downServer() {
        clients.forEach { socket, info ->
            runBlockingWithTimeout {
                info.output.writeObject(ServerDownMessage<T>())
                info.output.close()
            }
            socket.close()
        }
        clients.clear()
        serverSocketWithPort.socket.close()
    }

    private fun downClient(client: Socket) {
        clients.remove(client)
        client.close()
    }

    suspend fun securityCheck(clientInputChannel: ByteReadChannelWrapper): Boolean = true
    suspend fun serverHandshake(input: ByteReadChannelWrapper, output: ByteWriteChannelWrapper, log: Logger) = true

}

fun <T> runBlockingWithTimeout(timeout: Long = AUTH_TIMEOUT_IN_MILLISECONDS, block: suspend () -> T) =
    runBlocking { runWithTimeout(timeout = timeout) { block() } }

//@Throws(TimeoutException::class)
suspend fun <T> runWithTimeout(
    timeout: Long = AUTH_TIMEOUT_IN_MILLISECONDS,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    block: suspend () -> T
): T? = withTimeoutOrNull(timeout, unit) { block() }

//@Throws(ConnectionResetException::class)
suspend fun tryAcquireHandshakeMessage(input: ByteReadChannelWrapper, log: Logger): Boolean {
    log.info_and_print("tryAcquireHandshakeMessage")
    val bytes = runWithTimeout {
        input.nextBytes()
    } ?: return false.also { log.info_and_print("tryAcquireHandshakeMessage - FAIL") }
    log.info_and_print("bytes : ${bytes.toList()}")
    if (bytes.zip(FIRST_HANDSHAKE_BYTE_TOKEN).any { it.first != it.second }) {
        log.info_and_print("invalid token received")
        return false
    }
    log.info_and_print("tryAcquireHandshakeMessage - SUCCESS")
    return true
}


//@Throws(ConnectionResetException::class)
suspend fun trySendHandshakeMessage(output: ByteWriteChannelWrapper, log: Logger): Boolean {
    log.info_and_print("trySendHandshakeMessage")
    runWithTimeout {
        output.printBytesAndLength(FIRST_HANDSHAKE_BYTE_TOKEN.size, FIRST_HANDSHAKE_BYTE_TOKEN)
    } ?: return false.also { log.info_and_print("trySendHandshakeMessage - FAIL") }
    log.info_and_print("trySendHandshakeMessage - SUCCESS")
    return true
}