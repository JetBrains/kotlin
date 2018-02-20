package org.jetbrains.kotlin.daemon.common.experimental.socketInfrastructure

import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.daemon.common.experimental.LoopbackNetworkInterface
import java.beans.Transient
import java.io.Serializable
import java.net.InetSocketAddress

interface Client : Serializable {
    fun connectToServer()
    fun sendMessage(msg: Any): Deferred<Unit>
    fun <T> readMessage(): Deferred<T>
}

@Suppress("UNCHECKED_CAST")
class DefaultClient(
    val serverPort: Int,
    val serverHost : String? = null
) : Client {

    @Transient
    lateinit var input: ByteReadChannelWrapper
    @Transient
    lateinit var output: ByteWriteChannelWrapper

    override fun sendMessage(msg: Any) = async { output.writeObject(msg) }

    override fun <T> readMessage() = async { input.nextObject() as T }

    override fun connectToServer() {
        runBlocking {
            aSocket().tcp().connect(
                serverHost?.let { InetSocketAddress(it, serverPort) } ?: InetSocketAddress(serverPort)
            ).openIO().also {
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
}