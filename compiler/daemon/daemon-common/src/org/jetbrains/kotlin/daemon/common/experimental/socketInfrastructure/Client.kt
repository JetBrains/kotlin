package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.runBlocking
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
    fun connectToServer()

    fun sendMessage(msg: Server.AnyMessage<out ServerType>): Int // returns message unique id
    fun sendNoReplyMessage(msg: Server.AnyMessage<out ServerType>)
    fun <T> readMessage(id: Int): T

}

@Suppress("UNCHECKED_CAST")
abstract class DefaultAuthorizableClient<ServerType : ServerBase>(
    val serverPort: Int,
    val serverHost: String = LoopbackNetworkInterface.loopbackInetAddressName
) : Client<ServerType> {

    val log: Logger
        @Transient get() = Logger.getLogger("default client($serverPort)")

    @kotlin.jvm.Transient
    lateinit var input: ByteReadChannelWrapper

    @kotlin.jvm.Transient
    lateinit var output: ByteWriteChannelWrapper

    @kotlin.jvm.Transient
    private var socket: Socket? = null

    abstract suspend fun authorizeOnServer(serverOutputChannel: ByteWriteChannelWrapper): Boolean
    abstract suspend fun clientHandshake(input: ByteReadChannelWrapper, output: ByteWriteChannelWrapper, log: Logger): Boolean

    override fun close() {
//        try {
//            runBlockingWithTimeout {
//                sendNoReplyMessage(Server.EndConnectionMessage())
//            }
//        } catch (e: Throwable) {
//            log.info(e.message)
//        } finally {
//            socket?.close()
//        }
        socket?.close()
    }

    public class MessageReply<T : Any>(val messageId: Int, val reply: T?) : Serializable

    private interface ActorQuery
    private data class ExpectReplyQuery(val messageId: Int, val result: CompletableDeferred<MessageReply<*>>) : ActorQuery
    private class ReceiveReplyQuery : ActorQuery
    private data class SendMessageQuery(val message: Server.AnyMessage<*>, val messageId: CompletableDeferred<Int>) : ActorQuery
    private data class SendNoreplyMessageQuery(val message: Server.AnyMessage<*>) : ActorQuery

//    @kotlin.jvm.Transient
//    private lateinit var intermediateActor: SendChannel<ReceiveReplyQuery>

    @kotlin.jvm.Transient
    private lateinit var actor: SendChannel<ActorQuery>

    override fun sendMessage(msg: Server.AnyMessage<out ServerType>) = runBlocking {
        log.info("send message : $msg")
        val id = CompletableDeferred<Int>()
        actor.send(SendMessageQuery(msg, id))
        val idVal = id.await()
        log.info("idVal = $idVal")
        idVal
    }

    override fun sendNoReplyMessage(msg: Server.AnyMessage<out ServerType>) = runBlocking {
        log.info("sendNoReplyMessage $msg")
        log.info("actor: $actor")
        log.info("closed 4 send : ${actor.isClosedForSend}")
        actor.send(SendNoreplyMessageQuery(msg))
    }

    override fun <T> readMessage(id: Int): T = runBlocking {
        val result = CompletableDeferred<MessageReply<*>>()
        actor.send(ExpectReplyQuery(id, result))
        result.await().reply as T
    }

    override fun connectToServer() {
//        intermediateActor = actor(capacity = Channel.UNLIMITED) {
//            consumeEach { query ->
//                log.info("[${log.name}] : intermediateActor received $query")
//                actor.send(query)
//                log.info("[${log.name}] : query sent to actor!")
//            }
//        }
        actor = actor(capacity = 2) {
            val receivedMessages = hashMapOf<Int, MessageReply<*>>()
            val expectedMessages = hashMapOf<Int, ExpectReplyQuery>()
            var firstFreeMessageId = 0
            fun receiveReply() {
                log.info("[${log.name}] : got ReceiveReplyQuery")
                val replyAny = runBlocking { input.nextObject() } // TODO : support exception!!!
                if (replyAny !is MessageReply<*>) {
                    log.info("replyAny as MessageReply<*> - failed!")
                } else {
                    val reply = replyAny
                    log.info("[${log.name}] : received reply ${replyAny.reply} (id = ${replyAny.messageId})}")
                    expectedMessages[reply.messageId]?.also { expectedMsg ->
                        expectedMsg.result.complete(reply)
                    } ?: receivedMessages.put(reply.messageId, reply).also {
                        log.info("[${log.name}] : intermediateActor.send(ReceiveReplyQuery())")
                        receiveReply()
                    }
                }
            }
            for (query in channel) {
                when (query) {
                    is SendMessageQuery -> {
                        val id = firstFreeMessageId++
                        log.info("[${log.name}, ${this@DefaultAuthorizableClient}] : sending message : ${query.message} (predicted id = ${id})")
                        query.messageId.complete(id)
                        output.writeObject(query.message.withId(id)) // TODO : support exception!!!
                    }
                    is SendNoreplyMessageQuery -> {
                        log.info("[${log.name}] : sending noreply : ${query.message}")
                        output.writeObject(query.message.withId(-1))
                    }
                    is ExpectReplyQuery -> {
                        log.info("[${log.name}] : expect message with id = ${query.messageId}")
                        receivedMessages[query.messageId]?.also { reply ->
                            query.result.complete(reply)
                        } ?: expectedMessages.put(query.messageId, query).also {
                            log.info("[${log.name}] : intermediateActor.send(ReceiveReplyQuery())")
                            receiveReply()
                            //intermediateActor.send(ReceiveReplyQuery())
                        }
                    }
                    is ReceiveReplyQuery -> {
                        receiveReply()
                    }
                }
            }
//            consumeEach { query ->
//
//            }
        }

        runBlocking {
            log.info("connectToServer (port = $serverPort | host = $serverHost)")
            try {
                socket = LoopbackNetworkInterface.clientLoopbackSocketFactoryKtor.createSocket(
                    serverHost,
                    serverPort
                )
            } catch (e: Throwable) {
                log.info("EXCEPTION while connecting to server ($e)")
                close()
                throw e
            }
            log.info("connected (port = $serverPort, serv =$serverPort)")
            socket?.openIO(log)?.also {
                log.info("OK serv.openIO() |port=$serverPort|")
                input = it.input
                output = it.output
                if (!clientHandshake(input, output, log)) {
                    log.info("failed handshake($serverPort)")
                    close()
                    throw ConnectionResetException("failed to establish connection with server (handshake failed)")
                }
                if (!authorizeOnServer(output)) {
                    log.info("failed authorization($serverPort)")
                    close()
                    throw ConnectionResetException("failed to establish connection with server (authorization failed)")
                }
            }
        }
    }

    @Throws(ClassNotFoundException::class, IOException::class)
    private fun readObject(aInputStream: ObjectInputStream) {
        aInputStream.defaultReadObject()
        connectToServer()
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
}

class DefaultClientRMIWrapper<ServerType : ServerBase> : Client<ServerType> {
    override fun connectToServer() {}
    override fun sendMessage(msg: Server.AnyMessage<out ServerType>) =
        throw UnsupportedOperationException("sendMessage is not supported for RMI wrappers")

    override fun sendNoReplyMessage(msg: Server.AnyMessage<out ServerType>) =
        throw UnsupportedOperationException("sendMessage is not supported for RMI wrappers")

    override fun <T> readMessage(id: Int) = throw UnsupportedOperationException("readMessage is not supported for RMI wrappers")
    override fun close() {}
}