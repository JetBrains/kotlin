/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils

import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.configureDaemonOptions
import java.io.File

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class KotlinDaemonManager {

    companion object {
        fun getDaemon(messageCollector: MessageCollector, daemonJVMOptions: DaemonJVMOptions): Pair<CompileService, Int> {
            val compilerFullClasspath = listOf(
                File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler-embeddable/2.2.20-dev-7701/9021451596a52f412dcd5102a2f704938d596266/kotlin-compiler-embeddable-2.2.20-dev-7701.jar"),
                File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.20-dev-7701/fffea2c1b5c6a4ce552a4d7022922a3cec0da5ca/kotlin-stdlib-2.2.20-dev-7701.jar"),
                File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-reflect/2.2.20-dev-7701/4cd5aaa42b6fbbf485e54b89f8ca75698f3e53a3/kotlin-reflect-2.2.20-dev-7701.jar"),
                File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-script-runtime/2.2.20-dev-7701/f4beaeb6d5ae9fd4ff1535b4cf62a1741bc03c9f/kotlin-script-runtime-2.2.20-dev-7701.jar"),
                File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-daemon-embeddable/2.2.20-dev-7701/10e49a3d08d38f2cb042bf6d37109dd5d79073d7/kotlin-daemon-embeddable-2.2.20-dev-7701.jar"),
                File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.8.0/ac1dc37a30a93150b704022f8d895ee1bd3a36b3/kotlinx-coroutines-core-jvm-1.8.0.jar"),
                File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar")
            )

            val compilerId = CompilerId.makeCompilerId(compilerFullClasspath)
            val clientIsAliveFile = File("/Users/michal.svec/Desktop/runner/clientIsAliveFlagFile.alive")
            val sessionIsAliveFlagFile = File("/Users/michal.svec/Desktop/runner/sessionIsAliveFlagFile.alive")

            // Configure daemon options
            val daemonOptions = configureDaemonOptions()
            val (daemon, sessionId) = KotlinCompilerRunnerUtils.newDaemonConnection(
                compilerId,
                clientIsAliveFile,
                sessionIsAliveFlagFile,
                messageCollector = messageCollector,
                isDebugEnabled = true, // actually, prints daemon messages even unrelated to debug logs
                daemonJVMOptions = daemonJVMOptions,
                daemonOptions = daemonOptions,
            )!!


//
//            val compilationOptions = CompilationOptions(
//                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
//                targetPlatform = CompileService.TargetPlatform.JVM,
//                reportSeverity = 0,
//                reportCategories = arrayOf(),
//                requestedCompilationResults = arrayOf(),
//            )
//
//            val servicesFacade = BasicCompilerServicesWithResultsFacadeServer(messageCollector, null)
//
//            val result = daemon.compile(
//                sessionId = sessionId,
//                compilerArguments = arrayOf("/Users/michal.svec/Desktop/kotlin/remote-daemon/src/server.main/kotlin/input/Input.kt", "-cp", "/Users/michal.svec/Desktop/jars/kotlin-stdlib-2.2.0.jar", "-d", "/Users/michal.svec/Desktop/kotlin/remote-daemon/src/server.main/kotlin/output"),
//                compilationOptions = compilationOptions,
//                servicesFacade = servicesFacade,
//                compilationResults = null
//            )
//
//            println("call result is $result")
//            println("call result is ${result.get()}")
//            println("call result isGood ${result.isGood}")

            return Pair(daemon,sessionId)
        }


    }


}

fun main(){

    // Configure daemon JVM options
    val daemonJVMOptions = configureDaemonJVMOptions(
        inheritMemoryLimits = true,
        inheritOtherJvmOptions = false,
        inheritAdditionalProperties = true
    )
    val (daemon, sessionId) = KotlinDaemonManager.getDaemon(object: MessageCollector{
        override fun clear() {
        }

        override fun hasErrors(): Boolean {
            return false
        }

        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            println("message is $message")
        }
    }, daemonJVMOptions)

}