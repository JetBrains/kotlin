package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import org.jetbrains.kotlin.daemon.common.experimental.AUTH_TIMEOUT_IN_MILLISECONDS
import org.jetbrains.kotlin.daemon.common.experimental.FIRST_HANDSHAKE_BYTE_TOKEN
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.experimental.log
import sun.net.ConnectionResetException
import java.io.Serializable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
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
        if (!trySendHandshakeMessage(output) || !tryAcquireHandshakeMessage(input, log)) {
            log.info("failed to establish connection with client (handshake failed)")
            return@async Server.State.CLOSED
        }
        if (!securityCheck(input)) {
            log.info("failed to check securitay")
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

    fun securityCheck(clientInputChannel: ByteReadChannelWrapper): Boolean = true
}

@Throws(TimeoutException::class)
suspend fun <T> runWithTimeout(
    timeout: Long = AUTH_TIMEOUT_IN_MILLISECONDS,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    block: suspend () -> T
): T {
    val asyncRes = async { block() }
    delay(timeout, unit)
    return try {
        asyncRes.getCompleted()
    } catch (e: IllegalStateException) {
        throw TimeoutException("failed to get coroutine's value after given timeout")
    }
}

@Throws(ConnectionResetException::class)
suspend fun tryAcquireHandshakeMessage(input: ByteReadChannelWrapper, log: Logger) : Boolean {
    log.info("tryAcquireHandshakeMessage")
    val bytes: ByteArray = try {
        runWithTimeout {
            input.readBytes(FIRST_HANDSHAKE_BYTE_TOKEN.size)
        }
    } catch (e: TimeoutException) {
        log.info("no token received")
        return false
    }
    log.info("bytes : ${bytes.toList()}")
    if (bytes.zip(FIRST_HANDSHAKE_BYTE_TOKEN).any { it.first != it.second }) {
        log.info("invalid token received")
        return false
    }
    log.info("tryAcquireHandshakeMessage - SUCCESS")
    return true
}

@Throws(ConnectionResetException::class)
suspend fun trySendHandshakeMessage(output: ByteWriteChannelWrapper) : Boolean {
    log.info("trySendHandshakeMessage")
    try {
        runWithTimeout {
            output.printBytes(FIRST_HANDSHAKE_BYTE_TOKEN)
        }
    } catch (e: TimeoutException) {
        log.info("trySendHandshakeMessage - FAIL")
        return false
    }
    log.info("trySendHandshakeMessage - SUCCESS")
    return true
}