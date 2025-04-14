package org.jetbrains

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.bta.jvmCompilationOperation
import org.jetbrains.bta.jvmCompilerArguments


suspend fun main() {
    val server = KotlinToolchain.startLocalGrpcServer(8081)
    coroutineScope {
        launch {
            server.awaitTermination()
        }

        KotlinToolchain.connectRemoteGrpcToolchain("localhost", 8081).use { kotlinToolchain ->
            val operation = jvmCompilationOperation {
                compilerArguments = jvmCompilerArguments {
                    javaSources += "abc"
                }
            }

            val lookupsFlow = kotlinToolchain.jvmCompilerService.compile(operation)
            lookupsFlow.collect { println(it) }
        }
    }
}
