/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import io.grpc.ForwardingServerCall
import io.grpc.ForwardingServerCallListener
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class RemoteKotlinDaemonInterceptor : ServerInterceptor {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun debug(text: String){
        println("[${LocalDateTime.now().format(formatter)}] SERVER INTERCEPTOR: $text")
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT?, RespT?>?,
        headers: Metadata?,
        next: ServerCallHandler<ReqT?, RespT?>?,
    ): ServerCall.Listener<ReqT?>? {

        // SERVER ---> CLIENT
        val wrappedCall = object : ForwardingServerCall.SimpleForwardingServerCall<ReqT?, RespT?>(call) {
            override fun sendMessage(message: RespT?) {
                debug("sending message: $message")
                super.sendMessage(message)
            }

            override fun close(status: io.grpc.Status?, trailers: Metadata?) {
                debug("closing call with status: $status")
                super.close(status, trailers)
            }
        }

        val listener = next?.startCall(wrappedCall, headers)

        // CLIENT ---> SERVER
        return object : ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT?>(listener) {
            override fun onMessage(message: ReqT?) {
                debug("receiving message: ${message}")
                super.onMessage(message)
            }

            override fun onHalfClose() {
                debug("half closed")
                super.onHalfClose()
            }

            override fun onCancel() {
                debug("cancelled")
                super.onCancel()
            }

            override fun onComplete() {
                debug("completed")
                super.onComplete()
            }

            override fun onReady() {
                debug("ready")
                super.onReady()
            }
        }
    }
}