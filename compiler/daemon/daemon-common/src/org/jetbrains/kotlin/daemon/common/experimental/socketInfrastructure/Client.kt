package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.handleCoroutineException
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterface
import java.beans.Transient
import java.io.Serializable
import java.net.InetSocketAddress
import java.util.ArrayList
import java.util.function.Function

interface Client : Serializable {
    @Throws(Exception::class)
    fun connectToServer()
    fun sendMessage(msg: Any): Deferred<Unit>
    fun <T> readMessage(): Deferred<T>
}

@Suppress("UNCHECKED_CAST")
class DefaultClient(
    val serverPort: Int,
    val serverHost : String? = null
) : Client {

    lateinit var input: ByteReadChannelWrapper
        @Transient get
        @Transient set

    lateinit var output: ByteWriteChannelWrapper
        @Transient get
        @Transient set

    override fun sendMessage(msg: Any) = async { output.writeObject(msg) }

    override fun <T> readMessage() = async { input.nextObject() as T }

    override fun connectToServer() {
        runBlocking {
            Report.log("connectToServer(port =$serverPort | host = $serverHost)", "DefaultClient")
            try {
                aSocket().tcp().connect(
                    serverHost?.let { InetSocketAddress(it, serverPort) } ?: InetSocketAddress(serverPort)
                ).use { serv ->
                    Report.log("connected (port = $serverPort, serv =$serverPort)", "DefaultClient")
                    serv.openIO().also {
                        Report.log("OK serv.openIO() |port=$serverPort|", "DefaultClient")
                        input = it.input
                        output = it.output
                    }
                }
            } catch (e: Throwable) {
                Report.log("EXCEPTION ($e)", "DefaultClient")
                throw e
            }
        }
    }

}

class DefaultClientRMIWrapper : Client {
    override fun connectToServer() {}
    override fun sendMessage(msg: Any) = throw UnsupportedOperationException("sendMessage is not supported for RMI wrappers")
    override fun <T> readMessage() = throw UnsupportedOperationException("readMessage is not supported for RMI wrappers")
}