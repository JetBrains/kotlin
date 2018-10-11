package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterface
import sun.net.ConnectionResetException
import java.beans.Transient
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.logging.Logger

interface Client<ServerType : ServerBase> : Serializable, AutoCloseable {

    @Throws(Exception::class)
    suspend fun connectToServer()

    suspend fun sendMessage(msg: Server.AnyMessage<out ServerType>): Int // returns message unique id
    suspend fun sendNoReplyMessage(msg: Server.AnyMessage<out ServerType>)
    suspend fun <T> readMessage(id: Int): T

}

internal fun Logger.info_and_print(msg: String?) {
    this.info(msg)
//    println(msg)
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
    abstract suspend fun startKeepAlives()
    abstract suspend fun delayKeepAlives()

    override fun close() {
        socket?.close()
    }

    class MessageReply<T : Any>(val messageId: Int, val reply: T?) : Serializable

    protected interface ReadActorQuery
    protected data class ExpectReplyQuery(val messageId: Int, val result: CompletableDeferred<MessageReply<*>>) : ReadActorQuery
    protected class ReceiveReplyQuery(val reply: MessageReply<*>) : ReadActorQuery

    protected interface WriteActorQuery
    protected data class SendNoreplyMessageQuery(val message: Server.AnyMessage<*>) : WriteActorQuery
    protected data class SendMessageQuery(val message: Server.AnyMessage<*>, val messageId: CompletableDeferred<Any>) : WriteActorQuery

    protected class StopAllRequests : ReadActorQuery, WriteActorQuery

    @kotlin.jvm.Transient
    protected lateinit var readActor: SendChannel<ReadActorQuery>

    @kotlin.jvm.Transient
    private lateinit var writeActor: SendChannel<WriteActorQuery>

    override suspend fun sendMessage(msg: Server.AnyMessage<out ServerType>): Int {
        log.info_and_print("send message : $msg")
        val id = CompletableDeferred<Any>()
        writeActor.send(SendMessageQuery(msg, id))
        val idVal = id.await()
        if (idVal is IOException) {
            log.info_and_print("write exception : ${idVal.message}")
            throw idVal
        }
        log.info_and_print("idVal = $idVal")
        return idVal as Int
    }

    override suspend fun sendNoReplyMessage(msg: Server.AnyMessage<out ServerType>) {
        log.info_and_print("sendNoReplyMessage $msg")
        log.info_and_print("readActor: $readActor")
        log.info_and_print("closed 4 send : ${readActor.isClosedForSend}")
        writeActor.send(SendNoreplyMessageQuery(msg))
    }

    override suspend fun <T> readMessage(id: Int): T {
        log.info("readMessage with_id$id")
        val result = CompletableDeferred<MessageReply<*>>()
        log.info("result : $result with_id$id")
        try {
            readActor.send(ExpectReplyQuery(id, result))
        } catch (e: ClosedSendChannelException) {
            throw IOException("failed to read message (channel was closed)")
        }
        log.info("sent with_id$id")
        val actualResult = result.await().reply
        log.info("actualResult : $actualResult with_id$id")
        if (actualResult is IOException) {
            throw actualResult
        }
        return actualResult as T
    }

    override suspend fun connectToServer() {

        writeActor = actor(capacity = Channel.UNLIMITED) {
            var firstFreeMessageId = 0
            consumeEach { query ->
                when (query) {
                    is SendMessageQuery -> {
                        val id = firstFreeMessageId++
                        log.info_and_print("[${log.name}, ${this@DefaultAuthorizableClient}] : sending message : ${query.message} (predicted id = ${id})")
                        try {
                            output.writeObject(query.message.withId(id))
                            query.messageId.complete(id)
                        } catch (e: IOException) {
                            query.messageId.complete(e)
                        }
                    }
                    is SendNoreplyMessageQuery -> {
                        log.info_and_print("[${log.name}] : sending noreply : ${query.message}")
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
        val objectReaderActor = actor<NextObjectQuery>(capacity = Channel.UNLIMITED) {
            consumeEach {
                try {
                    val reply = input.nextObject()
                    if (reply is Server.ServerDownMessage<*>) {
                        throw IOException("connection closed by server")
                    } else if (reply !is MessageReply<*>) {
                        log.info_and_print("replyAny as MessageReply<*> - failed!")
                        throw IOException("contrafact message (expected MessageReply<*>)")
                    } else {
                        log.info_and_print("[${log.name}] : received reply ${reply.reply} (id = ${reply.messageId})}")
                        readActor.send(ReceiveReplyQuery(reply))
                    }
                } catch (e: IOException) {
                    readActor.send(StopAllRequests())
                }
            }
        }

        readActor = actor(capacity = Channel.UNLIMITED) {
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
                        log.info_and_print("[${log.name}] : expect message with id = ${query.messageId}")
                        receivedMessages[query.messageId]?.also { reply ->
                            query.result.complete(reply)
                        } ?: expectedMessages.put(query.messageId, query).also {
                            log.info_and_print("[${log.name}] : intermediateActor.send(ReceiveReplyQuery())")
                            objectReaderActor.send(nextObjectQuery)
                        }
                    }
                    is ReceiveReplyQuery -> {
                        val reply = query.reply
                        log.info_and_print("[${log.name}] : got ReceiveReplyQuery")
                        expectedMessages[reply.messageId]?.also { expectedMsg ->
                            expectedMsg.result.complete(reply)
                        } ?: receivedMessages.put(reply.messageId, reply).also {
                            log.info_and_print("[${log.name}] : intermediateActor.send(ReceiveReplyQuery())")
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



        log.info_and_print("connectToServer (port = $serverPort | host = $serverHost)")
        try {
            socket = LoopbackNetworkInterface.clientLoopbackSocketFactoryKtor.createSocket(
                serverHost,
                serverPort
            )
        } catch (e: Throwable) {
            log.info_and_print("EXCEPTION while connecting to server ($e)")
            close()
            throw e
        }
        log.info_and_print("connected (port = $serverPort, serv =$serverPort)")
        socket?.openIO(log)?.also {
            log.info_and_print("OK serv.openIO() |port=$serverPort|")
            input = it.input
            output = it.output
            if (!clientHandshake(input, output, log)) {
                log.info_and_print("failed handshake($serverPort)")
                throw ConnectionResetException("failed to establish connection with server (handshake failed)")
            }
            if (!authorizeOnServer(output)) {
                log.info_and_print("failed authorization($serverPort)")
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
    override suspend fun authorizeOnServer(output: ByteWriteChannelWrapper): Boolean = true
    override suspend fun startKeepAlives() {}
    override suspend fun delayKeepAlives() {}
}

class DefaultClientRMIWrapper<ServerType : ServerBase> : Client<ServerType> {

    override suspend fun connectToServer() {}
    override suspend fun sendMessage(msg: Server.AnyMessage<out ServerType>) =
        throw UnsupportedOperationException("sendMessage is not supported for RMI wrappers")

    override suspend fun sendNoReplyMessage(msg: Server.AnyMessage<out ServerType>) =
        throw UnsupportedOperationException("sendMessage is not supported for RMI wrappers")

    override suspend fun <T> readMessage(id: Int) = throw UnsupportedOperationException("readMessage is not supported for RMI wrappers")
    override fun close() {}
}