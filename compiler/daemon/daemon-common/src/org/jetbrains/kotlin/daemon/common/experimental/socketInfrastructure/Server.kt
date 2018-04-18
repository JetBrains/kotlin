package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeoutOrNull
import org.jetbrains.kotlin.daemon.common.experimental.AUTH_TIMEOUT_IN_MILLISECONDS
import org.jetbrains.kotlin.daemon.common.experimental.FIRST_HANDSHAKE_BYTE_TOKEN
import org.jetbrains.kotlin.daemon.common.experimental.ServerSocketWrapper
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

    val serverSocketWithPort: ServerSocketWrapper
    val serverPort: Int
        get() = serverSocketWithPort.port

    private val log: Logger
        get() = Logger.getLogger("default server($serverPort)")

    enum class State {
        WORKING, CLOSED, ERROR, DOWNING, UNVERIFIED
    }

    suspend fun processMessage(msg: AnyMessage<in T>, output: ByteWriteChannelWrapper): State = when (msg) {
        is Server.Message<in T> -> Server.State.WORKING.also { msg.process(this as T, output) }
        is Server.EndConnectionMessage<in T> -> {
            log.info("!EndConnectionMessage!")
            Server.State.CLOSED
        }
        is Server.ServerDownMessage<in T> -> Server.State.DOWNING
        else -> Server.State.ERROR
    }

    suspend fun attachClient(client: Socket): Deferred<State> = async {
        val (input, output) = client.openIO(log)
        if (!serverHandshake(input, output, log)) {
            log.info("failed to establish connection with client (handshake failed)")
            return@async Server.State.UNVERIFIED
        }
        if (!securityCheck(input)) {
            log.info("failed to check securitay")
            return@async Server.State.UNVERIFIED
        }
        log.info("   client verified ($client)")
        clients[client] = ClientInfo(client, input, output)
        log.info("   ($client)client in clients($clients)")
        var finalState = Server.State.WORKING
        loop@
        while (true) {
            log.info("   reading message from ($client)")
            val message = input.nextObject()
            if (message !is Server.AnyMessage<*>) {
                log.info("contrafact message")
                finalState = Server.State.ERROR
                break@loop
            }
            log.info("message ($client): $message")
            val state = processMessage(message as Server.AnyMessage<T>, output)
            when (state) {
                Server.State.WORKING -> continue@loop
                Server.State.ERROR -> {
                    log.info("ERROR after processing message")
                    finalState = Server.State.ERROR
                    break@loop
                }
                else -> {
                    finalState = state
                    break@loop
                }
            }
        }
        finalState
    }

    abstract class AnyMessage<ServerType : ServerBase> : Serializable {
        var messageId: Int? = null
        fun withId(id: Int): AnyMessage<ServerType> {
            messageId = id;
            return this
        }
    }

    abstract class Message<ServerType : ServerBase> : AnyMessage<ServerType>() {
        suspend fun process(server: ServerType, output: ByteWriteChannelWrapper) {
            async {
                processImpl(server, {
                    async { output.writeObject(DefaultAuthorizableClient.MessageReply(messageId ?: -1, it)) }
                })
            }
        }

        abstract suspend fun processImpl(server: ServerType, sendReply: (Any?) -> Unit)
    }

    class EndConnectionMessage<ServerType : ServerBase> : AnyMessage<ServerType>()

    class ServerDownMessage<ServerType : ServerBase> : AnyMessage<ServerType>()

    data class ClientInfo(val socket: Socket, val input: ByteReadChannelWrapper, val output: ByteWriteChannelWrapper)

    val clients: HashMap<Socket, ClientInfo>

    fun runServer(): Deferred<Unit> {
        log.info("binding to address(${serverSocketWithPort.port})")
        val serverSocket = serverSocketWithPort.socket
        return async {
            serverSocket.use {
                log.info("accepting clientSocket...")
                while (true) {
                    val client = serverSocket.accept()
                    log.info("client accepted! (${client.remoteAddress})")
                    async {
                        val state = attachClient(client).await()
                        log.info("finished ($client) with state : $state")
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
    }

    private fun downClient(client: Socket) {
        clients.remove(client)
        client.close()
    }

    suspend fun securityCheck(clientInputChannel: ByteReadChannelWrapper): Boolean = true
    suspend fun serverHandshake(input: ByteReadChannelWrapper, output: ByteWriteChannelWrapper, log: Logger) = true
}

fun <T> runBlockingWithTimeout(block: suspend () -> T) = runBlocking { runWithTimeout { block() } }

//@Throws(TimeoutException::class)
suspend fun <T> runWithTimeout(
    timeout: Long = AUTH_TIMEOUT_IN_MILLISECONDS,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    block: suspend () -> T
): T? = withTimeoutOrNull(timeout, unit) { block() }

//@Throws(ConnectionResetException::class)
suspend fun tryAcquireHandshakeMessage(input: ByteReadChannelWrapper, log: Logger): Boolean {
    log.info("tryAcquireHandshakeMessage")
    val bytes = runWithTimeout {
        input.nextBytes()
    } ?: return false.also { log.info("tryAcquireHandshakeMessage - FAIL") }
    log.info("bytes : ${bytes.toList()}")
    if (bytes.zip(FIRST_HANDSHAKE_BYTE_TOKEN).any { it.first != it.second }) {
        log.info("invalid token received")
        return false
    }
    log.info("tryAcquireHandshakeMessage - SUCCESS")
    return true
}


//@Throws(ConnectionResetException::class)
suspend fun trySendHandshakeMessage(output: ByteWriteChannelWrapper, log: Logger): Boolean {
    log.info("trySendHandshakeMessage")
    runWithTimeout {
        output.printBytesAndLength(FIRST_HANDSHAKE_BYTE_TOKEN.size, FIRST_HANDSHAKE_BYTE_TOKEN)
    } ?: return false.also { log.info("trySendHandshakeMessage - FAIL") }
    log.info("trySendHandshakeMessage - SUCCESS")
    return true
}