/*
 * Client-side interceptor for bidirectional streaming
 */

package client

import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RemoteClientInterceptor : ClientInterceptor {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    // grpc does not send default values therefore, they are not printed, we use
    // this printer that prints all fields of a message
    private val printer = JsonFormat.printer().alwaysPrintFieldsWithNoPresence()

    fun debug(text: String) {
        println("[${LocalDateTime.now().format(formatter)}] CLIENT INTERCEPTOR: $text")
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT?, RespT?>?,
        callOptions: CallOptions?,
        next: Channel?
    ): ClientCall<ReqT?, RespT?>? {
        val call = next?.newCall(method, callOptions)

        // CLIENT ---> SERVER
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT?, RespT?>(call) {
            override fun sendMessage(message: ReqT?) {
                debug("sending message: ${printer.print(message as MessageOrBuilder)}")
                super.sendMessage(message)
            }

            override fun halfClose() {
                debug("half closing")
                super.halfClose()
            }

            override fun cancel(message: String?, cause: Throwable?) {
                debug("cancelling")
                super.cancel(message, cause)
            }

            override fun start(responseListener: Listener<RespT?>?, headers: Metadata?) {

                // SERVER ---> CLIENT
                val wrappedListener = object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT?>(responseListener) {
                    override fun onMessage(message: RespT?) {
                        debug("receiving message: ${message}")
                        super.onMessage(message)
                    }

                    override fun onHeaders(headers: Metadata?) {
                        debug("receiving headers: ${headers}")
                        super.onHeaders(headers)
                    }

                    override fun onClose(status: io.grpc.Status?, trailers: Metadata?) {
                        debug("closing call with status: $status")
                        super.onClose(status, trailers)
                    }

                    override fun onReady() {
                        debug("ready")
                        super.onReady()
                    }
                }

                super.start(wrappedListener, headers)
            }
        }
    }
}
