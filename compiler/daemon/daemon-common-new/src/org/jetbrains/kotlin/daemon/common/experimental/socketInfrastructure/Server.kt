/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
            is Message<in T> -> State.WORKING.also {
                msg.process(this as T, output)
            }
            is EndConnectionMessage<in T> -> {
                State.CLOSED
            }
            is ServerDownMessage<in T> -> State.CLOSED
            else -> State.ERROR
        }

    // TODO: replace GlobalScope here and below with smth. more explicit
    fun attachClient(client: Socket): Deferred<State> = GlobalScope.async {
        val (input, output) = client.openIO(log)
        if (!serverHandshake(input, output, log)) {
            return@async State.UNVERIFIED
        }
        if (!checkClientCanReadFile(input)) {
            return@async State.UNVERIFIED
        }
        clients[client] = ClientInfo(client, input, output)
        var finalState = State.WORKING
        val keepAliveAcknowledgement = KeepAliveAcknowledgement<T>()
        loop@
        while (true) {
            val message = input.nextObject()
            when (message) {
                is ServerDownMessage<*> -> {
                    shutdownClient(client)
                    break@loop
                }
                is KeepAliveMessage<*> -> State.WORKING.also {
                    output.writeObject(
                        DefaultAuthorizableClient.MessageReply(
                            message.messageId!!,
                            keepAliveAcknowledgement
                        )
                    )
                }
                !is AnyMessage<*> -> {
                    finalState = State.ERROR
                    break@loop
                }
                else -> {
                    val state = processMessage(message as AnyMessage<T>, output)
                    when (state) {
                        State.WORKING -> continue@loop
                        State.ERROR -> {
                            finalState = State.ERROR
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
        fun process(server: ServerType, output: ByteWriteChannelWrapper) = GlobalScope.async {
            log.fine("$server starts processing ${this@Message}")
            processImpl(server, {
                log.fine("$server finished processing ${this@Message}, sending output")
                GlobalScope.async {
                    log.fine("$server starts sending ${this@Message} to output")
                    output.writeObject(DefaultAuthorizableClient.MessageReply(messageId ?: -1, it))
                    log.fine("$server finished sending ${this@Message} to output")
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

    private fun dealWithClient(client: Socket) = GlobalScope.async {
        val state = attachClient(client).await()
        when (state) {
            State.CLOSED, State.UNVERIFIED -> shutdownClient(client)
            State.DOWNING -> shutdownServer()
            else -> shutdownClient(client)
        }
    }

    fun runServer(): Deferred<Unit> {
        val serverSocket = serverSocketWithPort.socket
        return GlobalScope.async {
            serverSocket.use {
                while (true) {
                    dealWithClient(serverSocket.accept())
                }
            }
        }
    }

    fun shutdownServer() {
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

    private fun shutdownClient(client: Socket) {
        clients.remove(client)
        client.close()
    }

    /*
        This function writes some message in the server file, and awaits the confirmation from the client that it has read the message
        correctly. The purpose here is to check whether the client can actually access file system and read file contents.
    */
    suspend fun checkClientCanReadFile(clientInputChannel: ByteReadChannelWrapper): Boolean = true

    suspend fun serverHandshake(input: ByteReadChannelWrapper, output: ByteWriteChannelWrapper, log: Logger) = true

}

fun <T> runBlockingWithTimeout(timeout: Long = AUTH_TIMEOUT_IN_MILLISECONDS, block: suspend () -> T) =
    runBlocking { runWithTimeout(timeout = timeout) { block() } }

//@Throws(TimeoutException::class)
suspend fun <T> runWithTimeout(
    timeout: Long = AUTH_TIMEOUT_IN_MILLISECONDS,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    block: suspend CoroutineScope.() -> T
): T? = withTimeoutOrNull(unit.toMillis(timeout)) { block() }

//@Throws(ConnectionResetException::class)
suspend fun tryAcquireHandshakeMessage(input: ByteReadChannelWrapper): Boolean {
    val bytes = runWithTimeout {
        input.nextBytes()
    } ?: return false
    if (bytes.zip(FIRST_HANDSHAKE_BYTE_TOKEN).any { it.first != it.second }) {
        return false
    }
    return true
}


//@Throws(ConnectionResetException::class)
suspend fun trySendHandshakeMessage(output: ByteWriteChannelWrapper): Boolean {
    runWithTimeout {
        output.writeBytesAndLength(FIRST_HANDSHAKE_BYTE_TOKEN.size, FIRST_HANDSHAKE_BYTE_TOKEN)
    } ?: return false
    return true
}