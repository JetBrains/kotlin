/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package client

import client.auth.BasicHTTPAuthClient
import client.auth.CallAuthenticator
import common.RemoteCompilationService
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import model.CompileRequest
import model.CompileResponse
import model.toDomain
import model.toGrpc
import org.jetbrains.kotlin.server.CompileServiceGrpcKt
import java.io.Closeable
import java.util.concurrent.TimeUnit

class GrpcClientRemoteCompilationService : RemoteCompilationService, Closeable {

    private val channel = ManagedChannelBuilder
        .forAddress("localhost", 50051)
        .usePlaintext()
        .intercept(RemoteClientInterceptor())
        .build()

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
        return channelFlow {
            val grpcRequestFlow = compileRequests.map { it.toGrpc() }
            stub.compile(grpcRequestFlow).collect { send(it.toDomain()) }
        }
    }

    override fun close() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

}