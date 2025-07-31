/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.interceptors

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import server.auth.ServerAuth

class AuthInterceptor(private val authenticator: ServerAuth) : ServerInterceptor {

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT?, RespT?>?,
        headers: Metadata?,
        next: ServerCallHandler<ReqT?, RespT?>?,
    ): ServerCall.Listener<ReqT?>? {

        val credential = headers?.get(Metadata.Key.of("credential", Metadata.ASCII_STRING_MARSHALLER))

        if (credential == null || !authenticator.authenticate(credential)) {
            call?.close(io.grpc.Status.UNAUTHENTICATED, Metadata())
            return object : ServerCall.Listener<ReqT?>() {}
        }
        return next?.startCall(call, headers)
    }

}
