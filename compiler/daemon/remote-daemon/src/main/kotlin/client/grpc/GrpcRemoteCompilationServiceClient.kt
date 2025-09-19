/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package client.grpc

import client.core.BasicHTTPAuthClient
import client.core.Client
import com.google.protobuf.Empty
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import model.CompileRequest
import model.CompileResponse
import model.toDomain
import model.toProto
import org.jetbrains.kotlin.server.CompileServiceGrpcKt

class GrpcRemoteCompilationServiceClient(
    private val host: String,
    private val port: Int,
    private val logging: Boolean = false,
    private val channel: ManagedChannel
    = ManagedChannelBuilder
        .forAddress(host, port)
        .let { builder ->
            if (logging) builder.intercept(RemoteClientInterceptor())
            if (host == "localhost") builder.usePlaintext() else builder.useTransportSecurity()
        }
        .build()
) : Client {

    private val stub: CompileServiceGrpcKt.CompileServiceCoroutineStub = CompileServiceGrpcKt
        .CompileServiceCoroutineStub(channel)
        .withCallCredentials(
            CallAuthenticator(
                BasicHTTPAuthClient(
                    username = "admin",
                    password = "admin"
                )
            )
        )

    override fun compile(compileRequests: Flow<CompileRequest>): Flow<CompileResponse> {
        return stub.compile(compileRequests.map { it.toProto() }).map { it.toDomain() }
    }

    override suspend fun cleanup() {
        stub.cleanup(Empty.getDefaultInstance())
    }

    override fun close() {
        channel.shutdown()
    }

}