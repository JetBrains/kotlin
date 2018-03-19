package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.*
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterface
import java.beans.Transient
import java.io.Serializable
import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.function.Function
import java.util.logging.Logger

interface Client : Serializable, AutoCloseable {
    @Throws(Exception::class)
    fun connectToServer()

    fun sendMessage(msg: Any): Deferred<Unit>
    fun <T> readMessage(): Deferred<T>

    fun f() {}
}

@Suppress("UNCHECKED_CAST")
class DefaultClient(
    val serverPort: Int,
    val serverHost: String? = null
) : Client {

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

    override fun close() {
        socket?.close()
    }

    override fun sendMessage(msg: Any) = async { output.writeObject(msg) }

    override fun <T> readMessage() = async { input.nextObject() as T }

    override fun connectToServer() {
        runBlocking(Unconfined) {
            log.info("connectToServer(port =$serverPort | host = $serverHost)")
            try {
                socket = aSocket().tcp().connect(InetSocketAddress(serverPort))
            } catch (e: Throwable) {
                log.info("EXCEPTION ($e)")
                throw e
            }
            log.info("connected (port = $serverPort, serv =$serverPort)")
            socket!!.openIO(log).also {
                log.info("OK serv.openIO() |port=$serverPort|")
                input = it.input
                output = it.output
            }
        }
    }

}

class DefaultClientRMIWrapper : Client {
    override fun connectToServer() {}
    override fun sendMessage(msg: Any) = throw UnsupportedOperationException("sendMessage is not supported for RMI wrappers")
    override fun <T> readMessage() = throw UnsupportedOperationException("readMessage is not supported for RMI wrappers")
    override fun close() {}
}