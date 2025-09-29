/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmark

import client.core.RemoteCompilationClient
import kotlinx.coroutines.delay

import kotlinx.serialization.ExperimentalSerializationApi
import model.CompilationResultSource
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import java.io.File
import kotlin.time.Duration
import kotlin.time.TimeSource

enum class RemoteCompilationServiceImplType {
    GRPC,
    KOTLINX_RPC
}

@OptIn(ExperimentalSerializationApi::class)
class Benchmark(
    val implType: RemoteCompilationServiceImplType,
    val host: String,
    val port: Int
) {

    private suspend fun compileProject(client: RemoteCompilationClient, tasks: List<Task>) {
        val compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
            reportSeverity = 1,
            reportCategories = arrayOf(0, 1, 2, 3, 4),
            requestedCompilationResults = arrayOf(),
        )

        var fromCompiler = 0
        var fromCache = 0
        tasks.forEachIndexed { index, task ->

            val res = client.compile(
                "ktor-task-$index",
                task.compilerArgs,
                compilationOptions,
            )

            if (res.exitCode != 0) {
                throw IllegalStateException("Thrown: Compilation number $index failed with exit code ${res.exitCode}")
            }
            if (res.compilationResultSource == CompilationResultSource.CACHE) {
                fromCache++
                println("Compilation $index finished with exit code ${res.exitCode}, from cache")
            } else {
                fromCompiler++
                println("Compilation $index finished with exit code ${res.exitCode}, from compiler")
            }
        }
        println("${tasks.size} tasks compiled from cache: $fromCache, from compiler: $fromCompiler")
    }

    suspend fun run(
        iterations: Int,
        tasks: List<Task>
    ) {
        val times = mutableListOf<Duration>()

        for (i in 0..<iterations) {
            println("run number: $i")
            val client = RemoteCompilationClient.getClient(implType, host, port)
            client.cleanup()
            client.close()
            val warmupDuration = measureSuspend {
                val client = RemoteCompilationClient.getClient(implType, host, port)
                compileProject(client, tasks)
            }
            times.add(warmupDuration)
            println("times: $times")
            println("average: ${times.reduce { acc, duration -> acc + duration } / (i+1)}")
            if (times.size > 1)
                println(
                    "average without warmup ${
                        times.subList(1, times.size).reduce { acc, duration -> acc + duration } / i
                    }")
        }
    }

    suspend fun measureSuspend(block: suspend () -> Unit): Duration {
        val mark = TimeSource.Monotonic.markNow()
        block()
        return mark.elapsedNow()
    }
}

suspend fun main() {
    val ktorTasks = TasksExtractor.getTasks("/Users/michal.svec/Desktop/ktor/output").toMutableList()
    ktorTasks.removeAt(44) // this task requires some compiler plugin incompatible with our compiler version

    val localPort = 8000
    val localHost = "localhost"

    val remotePort = 443 // because SSL
    val remoteWebsocketsHost = "remote-kotlin-daemon-websockets.labs.jb.gg"
    val remoteGrpcHost = "remote-kotlin-daemon-grpc.labs.jb.gg"

    Benchmark(RemoteCompilationServiceImplType.GRPC, remoteGrpcHost, remotePort).run(16, ktorTasks)
}