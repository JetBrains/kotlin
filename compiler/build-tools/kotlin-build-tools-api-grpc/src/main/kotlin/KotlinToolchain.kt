package org.jetbrains

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.bta.*
import org.jetbrains.bta.Bta.*

//
//class KotlinToolchainNoGrpc {
//    fun executeOperation(buildOperation: BuildOperation) {
//        // do something with buildOperation
//        when (buildOperation.operationCase) {
//            BuildOperation.OperationCase.JVM_COMPILATION -> println("JVM")
//            BuildOperation.OperationCase.JS_COMPILATION -> println("JS")
//            BuildOperation.OperationCase.OPERATION_NOT_SET -> println("NOT_SET")
//        }
//    }
//}
//
//fun KotlinToolchainNoGrpc.executeOperation(block: BuildOperationKt.Dsl.() -> Unit) {
//    return executeOperation(buildOperation(block))
//}


// with GRPC
class JvmToolchainImpl() : JvmToolchainGrpcKt.JvmToolchainCoroutineImplBase() {
    override fun compile(request: JvmCompilationOperation): Flow<JvmCompilationOperationResult> {
        return flow {
            // invoke compiler and emit some lookups

            emit(jvmCompilationOperationResult {
                lookup = lookupRecord {
                    name = "aaa"
                    scopeKind = ScopeKind.PACKAGE
                }
            })
            delay(2000)
            emit(jvmCompilationOperationResult {
                lookup = lookupRecord {
                    name = "bbb"
                    scopeKind = ScopeKind.CLASSIFIER
                }
            })
        }
    }
}

class KotlinToolchain private constructor(
    private val channel: ManagedChannel,
    private val cleanup: () -> Unit = {},
) : AutoCloseable {

    val jvmCompilerService by lazy { JvmToolchainGrpcKt.JvmToolchainCoroutineStub(channel) }
    val jsCompilerService by lazy { JsToolchainGrpcKt.JsToolchainCoroutineStub(channel) }
    val nativeCompilerService by lazy { NativeToolchainGrpcKt.NativeToolchainCoroutineStub(channel) }

    override fun close() {
        channel.shutdown()
        cleanup()
    }

    companion object {
        @JvmStatic
        fun startLocalGrpcServer(port: Int): Server {
            val server: Server = ServerBuilder.forPort(port).addService(JvmToolchainImpl()).build()
            server.start()
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    println("*** shutting down gRPC server since JVM is shutting down")
                    server.shutdown()
                    println("*** server shut down")
                })
            return server
        }

        @JvmStatic
        fun connectRemoteGrpcToolchain(host: String, port: Int): KotlinToolchain {
            return KotlinToolchain(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build())
        }

        @JvmStatic
        fun createInProcess(): KotlinToolchain {
            val uniqueName = InProcessServerBuilder.generateName()
            val server: Server = InProcessServerBuilder.forName(uniqueName).addService(JvmToolchainImpl()).build()
            server.start()
            Runtime.getRuntime().addShutdownHook(
                Thread {
                    println("*** shutting down gRPC server '${uniqueName}' since JVM is shutting down")
                    server.shutdown()
                    println("*** server '${uniqueName}' shut down")
                })
            return KotlinToolchain(
                channel = InProcessChannelBuilder.forName(uniqueName).build(), cleanup = { server.shutdown() })
        }
    }
}