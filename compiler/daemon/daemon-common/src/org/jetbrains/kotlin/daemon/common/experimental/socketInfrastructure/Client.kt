package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
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

    fun sendMessage(msg: Server.AnyMessage<out ServerType>): Deferred<Unit>
    fun <T> readMessage(): Deferred<T>
}

@Suppress("UNCHECKED_CAST")
abstract class DefaultAuthorizableClient<ServerType: ServerBase>(
    val serverPort: Int,
    val serverHost: String = LoopbackNetworkInterface.loopbackInetAddressName
) : Client<ServerType> {

    val log: Logger
        @Transient get() = Logger.getLogger("default client")

    lateinit var input: ByteReadChannelWrapper
        @Transient get
        @Transient set

    lateinit var output: ByteWriteChannelWrapper
        @Transient get
        @Transient set

    private var socket: Socket? = null
        @Transient get
        @Transient set

    abstract fun authorizeOnServer(serverOutputChannel: ByteWriteChannelWrapper)

    override fun close() {
        runBlocking {
            output.writeObject(Server.EndConnectionMessage<ServerType>())
        }
        socket?.close()
    }

    override fun sendMessage(msg: Server.AnyMessage<out ServerType>) = async { output.writeObject(msg) }

    override fun <T> readMessage() = async { input.nextObject() as T }

    @Throws(Exception::class)
    override fun connectToServer() {
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
            socket!!.openIO(log).also {
                log.info("OK serv.openIO() |port=$serverPort|")
                input = it.input
                output = it.output
                if (!trySendHandshakeMessage(output) || !tryAcquireHandshakeMessage(input, log)) {
                    close()
                    throw ConnectionResetException("failed to establish connection with server (handshake failed)")
                }
                try {
                    authorizeOnServer(output)
                } catch (e: Exception) {
                    close()
                    throw e
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

class DefaultClient<ServerType: ServerBase>(
    serverPort: Int,
    serverHost: String = LoopbackNetworkInterface.loopbackInetAddressName
) : DefaultAuthorizableClient<ServerType>(serverPort, serverHost) {
    override fun authorizeOnServer(output: ByteWriteChannelWrapper) {}
}

class DefaultClientRMIWrapper<ServerType: ServerBase> : Client<ServerType> {
    override fun connectToServer() {}
    override fun sendMessage(msg: Server.AnyMessage<out ServerType>) = throw UnsupportedOperationException("sendMessage is not supported for RMI wrappers")
    override fun <T> readMessage() = throw UnsupportedOperationException("readMessage is not supported for RMI wrappers")
    override fun close() {}
}