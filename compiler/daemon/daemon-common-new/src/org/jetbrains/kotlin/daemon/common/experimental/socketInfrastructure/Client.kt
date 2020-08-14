/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.*
import org.jetbrains.kotlin.daemon.common.LoopbackNetworkInterface
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterfaceKtor
import sun.net.ConnectionResetException
import java.beans.Transient
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.logging.Logger
import org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure.Server.*

interface Client<ServerType : ServerBase> : Serializable, AutoCloseable {

    @Throws(Exception::class)
    suspend fun connectToServer()

    suspend fun sendMessage(msg: AnyMessage<out ServerType>): Int // returns message unique id
    fun sendNoReplyMessage(msg: AnyMessage<out ServerType>)
    suspend fun <T> readMessage(id: Int): T

}

@Suppress("UNCHECKED_CAST")
abstract class DefaultAuthorizableClient<ServerType : ServerBase>(
    val serverPort: Int,
    val serverHost: String = LoopbackNetworkInterface.loopbackInetAddressName
) : Client<ServerType> {

    val log: Logger
        @Transient get() = Logger.getLogger("default client($serverPort)")//.also { it.setUseParentHandlers(false); }

    @kotlin.jvm.Transient
    lateinit var input: ByteReadChannelWrapper

    @kotlin.jvm.Transient
    lateinit var output: ByteWriteChannelWrapper

    @kotlin.jvm.Transient
    private var socket: Socket? = null

    abstract suspend fun authorizeOnServer(serverOutputChannel: ByteWriteChannelWrapper): Boolean
    abstract suspend fun clientHandshake(input: ByteReadChannelWrapper, output: ByteWriteChannelWrapper, log: Logger): Boolean

    /*
     The purpose of ths function is the following : a client starts sending keep-alive requests to the server.
     It sends a request every K seconds and awaits a response after K/2 seconds.
     In case of not receiving the response from the server we stop waiting for other responses.
     This resolves the case when the server is down but the client is still waiting for some calculations.
     Keep-alives run in a separate thread on client and on server, hence we can assume that the calculations on server
     shouldn't delay keep-alive responses too much
    */
    abstract fun startKeepAlives()

    /*
     `delayKeepAlives` resolves another issue. Imagine that a server and a client have too many short requests/responses,
     say, 1000 requests/responses and 0.001 seconds of latency. Then we can miss keep-alive response because of a socket workload,
     or, say, if a scheduler decides not to schedule on a keep-alive thread.
     In this case we say that any response can stand for a keep-alive message, as well. delayKeepAlives() serves this purpose.
    */
    abstract fun delayKeepAlives()

    override fun close() {
        socket?.close()
    }

    class MessageReply<T : Any>(val messageId: Int, val reply: T?) : Serializable

    protected interface ReadActorQuery
    protected data class ExpectReplyQuery(val messageId: Int, val result: CompletableDeferred<MessageReply<*>>) : ReadActorQuery
    protected class ReceiveReplyQuery(val reply: MessageReply<*>) : ReadActorQuery

    protected interface WriteActorQuery
    protected data class SendNoreplyMessageQuery(val message: AnyMessage<*>) : WriteActorQuery
    protected data class SendMessageQuery(val message: AnyMessage<*>, val messageId: CompletableDeferred<Any>) : WriteActorQuery

    protected class StopAllRequests : ReadActorQuery, WriteActorQuery

    @kotlin.jvm.Transient
    protected lateinit var readActor: SendChannel<ReadActorQuery>

    @kotlin.jvm.Transient
    private lateinit var writeActor: SendChannel<WriteActorQuery>

    override suspend fun sendMessage(msg: AnyMessage<out ServerType>): Int {
        val id = CompletableDeferred<Any>()
        writeActor.send(SendMessageQuery(msg, id))
        val idVal = id.await()
        if (idVal is IOException) {
            throw idVal
        }
        return idVal as Int
    }

    override fun sendNoReplyMessage(msg: AnyMessage<out ServerType>) {
        writeActor.offer(SendNoreplyMessageQuery(msg))
    }

    override suspend fun <T> readMessage(id: Int): T {
        val result = CompletableDeferred<MessageReply<*>>()
        try {
            readActor.send(ExpectReplyQuery(id, result))
        } catch (e: ClosedSendChannelException) {
            throw IOException("failed to read message (channel was closed)")
        }
        val actualResult = result.await().reply
        if (actualResult is IOException) {
            throw actualResult
        }
        return actualResult as T
    }

    @OptIn(ObsoleteCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    override suspend fun connectToServer() {

        writeActor = GlobalScope.actor(capacity = Channel.UNLIMITED) {
            var firstFreeMessageId = 0
            consumeEach { query ->
                when (query) {
                    is SendMessageQuery -> {
                        val id = firstFreeMessageId++
                        try {
                            output.writeObject(query.message.withId(id))
                            query.messageId.complete(id)
                        } catch (e: IOException) {
                            query.messageId.complete(e)
                        }
                    }
                    is SendNoreplyMessageQuery -> {
                        output.writeObject(query.message.withId(-1))
                    }
                    is StopAllRequests -> {
                        channel.close()
                    }
                }
            }
        }

        class NextObjectQuery

        val nextObjectQuery = NextObjectQuery()
        val objectReaderActor = GlobalScope.actor<NextObjectQuery>(capacity = Channel.UNLIMITED) {
            consumeEach {
                try {
                    val reply = input.nextObject()
                    when (reply) {
                        is ServerDownMessage<*> -> throw IOException("connection closed by server")
                        !is MessageReply<*> -> throw IOException("contrafact message (expected MessageReply<*>)")
                        else -> readActor.send(ReceiveReplyQuery(reply))
                    }
                } catch (e: IOException) {
                    readActor.send(StopAllRequests())
                }
            }
        }

        readActor = GlobalScope.actor(capacity = Channel.UNLIMITED) {
            val receivedMessages = hashMapOf<Int, MessageReply<*>>()
            val expectedMessages = hashMapOf<Int, ExpectReplyQuery>()

            fun broadcastIOException(e: IOException) {
                channel.close()
                expectedMessages.forEach { id, deferred ->
                    deferred.result.complete(MessageReply(id, e))
                }
                expectedMessages.clear()
                receivedMessages.clear()
            }

            consumeEach { query ->
                when (query) {
                    is ExpectReplyQuery -> {
                        receivedMessages[query.messageId]?.also { reply ->
                            query.result.complete(reply)
                        } ?: expectedMessages.put(query.messageId, query).also {
                            objectReaderActor.send(nextObjectQuery)
                        }
                    }
                    is ReceiveReplyQuery -> {
                        val reply = query.reply
                        expectedMessages[reply.messageId]?.also { expectedMsg ->
                            expectedMsg.result.complete(reply)
                        } ?: receivedMessages.put(reply.messageId, reply).also {
                            objectReaderActor.send(nextObjectQuery)
                        }
                        delayKeepAlives()
                    }
                    is StopAllRequests -> {
                        broadcastIOException(IOException("KeepAlive failed"))
                        writeActor.send(StopAllRequests())
                    }
                }
            }
        }



        try {
            socket = LoopbackNetworkInterfaceKtor.clientLoopbackSocketFactoryKtor.createSocket(
                serverHost,
                serverPort
            )
        } catch (e: Throwable) {
            close()
            throw e
        }
        socket?.openIO(log)?.also {
            input = it.input
            output = it.output
            if (!clientHandshake(input, output, log)) {
                throw ConnectionResetException("failed to establish connection with server (handshake failed)")
            }
            if (!authorizeOnServer(output)) {
                throw ConnectionResetException("failed to establish connection with server (authorization failed)")
            }
        }

        startKeepAlives()

    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(aInputStream: ObjectInputStream) {
        aInputStream.defaultReadObject()
        println("connecting...")
        runBlocking { connectToServer() }
        println("connectED")
    }

    @Throws(IOException::class)
    private fun writeObject(aOutputStream: ObjectOutputStream) {
        aOutputStream.defaultWriteObject()
    }

}

class DefaultClient<ServerType : ServerBase>(
    serverPort: Int,
    serverHost: String = LoopbackNetworkInterface.loopbackInetAddressName
) : DefaultAuthorizableClient<ServerType>(serverPort, serverHost) {
    override suspend fun clientHandshake(input: ByteReadChannelWrapper, output: ByteWriteChannelWrapper, log: Logger) = true
    override suspend fun authorizeOnServer(serverOutputChannel: ByteWriteChannelWrapper): Boolean = true
    override fun startKeepAlives() {}
    override fun delayKeepAlives() {}
}

class DefaultClientRMIWrapper<ServerType : ServerBase> : Client<ServerType> {

    override suspend fun connectToServer() {}
    override suspend fun sendMessage(msg: AnyMessage<out ServerType>) =
        throw UnsupportedOperationException("sendMessage is not supported for RMI wrappers")

    override fun sendNoReplyMessage(msg: AnyMessage<out ServerType>) =
        throw UnsupportedOperationException("sendMessage is not supported for RMI wrappers")

    override suspend fun <T> readMessage(id: Int) = throw UnsupportedOperationException("readMessage is not supported for RMI wrappers")
    override fun close() {}
}