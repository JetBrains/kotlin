package org.jetbrains

import org.jetbrains.bta.jvmCompilationOperation
import org.jetbrains.bta.jvmCompilerArguments


suspend fun main() {
    KotlinToolchain.createInProcess().use { kotlinToolchain ->
        val operation = jvmCompilationOperation {
            compilerArguments = jvmCompilerArguments {
                javaSources += "abc"

            }
        }
        val lookupsFlow = kotlinToolchain.jvmCompilerService.compile(operation)
        lookupsFlow.collect { println(it) }
    }
}
