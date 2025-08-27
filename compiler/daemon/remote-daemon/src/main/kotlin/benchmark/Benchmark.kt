/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmark

import common.RemoteCompilationServiceImplType
import kotlinx.coroutines.delay
import main.kotlin.server.RemoteCompilationServer
import org.jetbrains.kotlin.client.RemoteCompilationClient
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode


class Benchmark(
    serverImplType: RemoteCompilationServiceImplType
) {

    val client = RemoteCompilationClient(serverImplType, logging = true)

    init {
        RemoteCompilationServer(50051, serverImplType, logging = false).start()
    }

    suspend fun compileProject(tasks: List<Task>) {
        val compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
            reportSeverity = 0,
            reportCategories = arrayOf(),
            requestedCompilationResults = arrayOf(),
        )

        tasks.forEachIndexed { index, task ->

            val res = client.compile(
                "ktor-task-$index",
                task.compilerArgs,
                compilationOptions,
            )

            println("Compilation number $index finished with exit code ${res.exitCode}")
            if (res.exitCode != 0) {
                throw IllegalStateException("Thrown: Compilation number $index failed with exit code ${res.exitCode}")
            }
        }
    }
}

suspend fun main() {
    val ktorTasks =
        DataExtractor.getTask("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/benchmark/compileOutput")
    println("We have ${ktorTasks.size} tasks to compile")
    println("************* BENCHMARK INITIALIZED *************")
    val benchmark = Benchmark(
        RemoteCompilationServiceImplType.GRPC
    )
    benchmark.compileProject(ktorTasks)
}