package com.example

import client.core.Client
import common.RemoteCompilationService
import io.ktor.client.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.withService
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.KrpcSerialFormatConfiguration

import model.CompileRequest
import model.CompileResponse

class KotlinxRpcRemoteCompilationServiceClient(
    private val host: String = "localhost",
    private val port: Int,
    private val serialization: KrpcSerialFormatConfiguration.() -> Unit
) : Client {

    val ktorClient = HttpClient {
        installKrpc {
            waitForServices = true
        }
    }

    val client: KtorRpcClient = ktorClient.rpc {
        url {
            host = this@KotlinxRpcRemoteCompilationServiceClient.host
            port = this@KotlinxRpcRemoteCompilationServiceClient.port
            encodedPath = "compile"
        }
        url {
            val useTls = this@KotlinxRpcRemoteCompilationServiceClient.port == 443
            protocol = if (useTls) URLProtocol.WSS else URLProtocol.WS
            host = this@KotlinxRpcRemoteCompilationServiceClient.host
            port = this@KotlinxRpcRemoteCompilationServiceClient.port
            encodedPath = "/compile"
        }
        rpcConfig {
            serialization {
                this.serialization()
            }
        }

    }

    val compilationService: RemoteCompilationService = client.withService<RemoteCompilationService>()

    override fun compile(compileRequests: Flow<CompileRequest>): Flow<CompileResponse> {
        return compilationService.compile(compileRequests)
    }

    override suspend fun cleanup() {
        compilationService.cleanup()
    }

    override fun close() {
        ktorClient.close()
    }

}
