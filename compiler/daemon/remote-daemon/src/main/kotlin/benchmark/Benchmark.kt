/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package benchmark

import main.kotlin.server.RemoteCompilationServer
import client.core.RemoteCompilationClient
import common.CLIENT_COMPILED_DIR
import common.CLIENT_TMP_DIR
import common.SERVER_CACHE_DIR
import common.SERVER_COMPILATION_WORKSPACE_DIR
import kotlinx.serialization.ExperimentalSerializationApi
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode

enum class RemoteCompilationServiceImplType {
    GRPC,
    KOTLINX_RPC
}

@OptIn(ExperimentalSerializationApi::class)
class Benchmark {

    suspend fun compileProject(client: RemoteCompilationClient, tasks: List<Task>) {
        client.cleanup() // TODO consider removing, this was just fro JMH multiple iterations
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
// HOW TO RUN
// 1. add this task to build.gradle of Ktor project
//tasks.register("assembleAllKotlin") {
//    allprojects {
//        dependsOn(tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>())
//    }
//}
//
// 2. run this command that redirects all output to a file
// ./gradlew --stop && ./gradlew clean && ./gradlew assembleAllKotlin --no-configuration-cache --rerun --no-build-cache --refresh-dependencies -Pkotlin.internal.compiler.arguments.log.level=warning -Pkotlin.incremental=false > output
//
// 3. create a file in this Kotlin project and paste there the generated output from previous step
//
// 4. pass the path of the file to getTask function
//
// 5. run this command to build required modules for our app
// ./gradlew :libraries:build && ./gradlew :kotlin-scripting-compiler-impl-embeddable:build && ./gradlew :kotlin-scripting-compiler-impl:build && ./gradlew :kotlin-scripting-compiler-embeddable:build && ./gradlew :kotlinx-serialization-compiler-plugin.embeddable:build
//
// 6. swap paths of hardcoded dependencies for yours in CompilerUtils.getMap() function, starting on line 89
//
// 7. change CWD of this configuration to /kotlin/compiler/daemon/remote-daemon
//
// 8. ready to run the main function

suspend fun main() {
    val ktorTasks =
        TasksExtractor.getTasks("/Users/michal.svec/Desktop/ktor/outputlatest")
    println("We have ${ktorTasks.size} tasks to compile")
    val localPort = 8000
    val localHost = "localhost"

    val remotePort = 443
    val remoteHost = "remote-kotlin-daemon.labs.jb.gg"


    val implType = RemoteCompilationServiceImplType.GRPC
//    val server = RemoteCompilationServer.getServer(implType, localPort, logging = true)
    val client = RemoteCompilationClient.getClient(implType, remoteHost, remotePort)

//    server.start(block = false)
    val benchmark = Benchmark()
    benchmark.compileProject(client, ktorTasks.subList(10, ktorTasks.size))
//    server.stop()

    SERVER_CACHE_DIR.toFile().deleteRecursively()
    SERVER_COMPILATION_WORKSPACE_DIR.toFile().deleteRecursively()
    CLIENT_TMP_DIR.toFile().deleteRecursively()
    CLIENT_COMPILED_DIR.toFile().deleteRecursively()
}