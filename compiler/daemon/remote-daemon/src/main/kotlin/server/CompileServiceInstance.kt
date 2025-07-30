
package server

import common.OUTPUT_FILES_DIR
import common.buildAbsPath
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.daemon.CompileServiceImpl
import org.jetbrains.kotlin.daemon.CompilerSelector
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_FIND_PORT_ATTEMPTS
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_PORTS_RANGE_END
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_PORTS_RANGE_START
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_TIMEOUT_INFINITE_MS
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.daemon.common.configureDaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.findPortAndCreateRegistry
import java.io.File
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.utils.KotlinPaths
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.system.exitProcess


fun getCompileService(): CompileService {
    val compilerSelector = object : CompilerSelector {
        private val jvm by lazy<K2JVMCompiler> { K2JVMCompiler() }
        private val js by lazy<K2JSCompiler> { K2JSCompiler() }
        private val metadata by lazy<KotlinMetadataCompiler> { KotlinMetadataCompiler() }
        override fun get(targetPlatform: CompileService.TargetPlatform): CLICompiler<*> = when (targetPlatform) {
            CompileService.TargetPlatform.JVM -> jvm
            CompileService.TargetPlatform.JS -> js
            CompileService.TargetPlatform.METADATA -> metadata
        }
    }
//    val compilerId = CompilerId()
//    val compilerFullClasspath = listOf(
//        File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-compiler-embeddable/2.2.20-dev-7701/9021451596a52f412dcd5102a2f704938d596266/kotlin-compiler-embeddable-2.2.20-dev-7701.jar"),
//        File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.20-dev-7701/fffea2c1b5c6a4ce552a4d7022922a3cec0da5ca/kotlin-stdlib-2.2.20-dev-7701.jar"),
//        File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-reflect/2.2.20-dev-7701/4cd5aaa42b6fbbf485e54b89f8ca75698f3e53a3/kotlin-reflect-2.2.20-dev-7701.jar"),
//        File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-script-runtime/2.2.20-dev-7701/f4beaeb6d5ae9fd4ff1535b4cf62a1741bc03c9f/kotlin-script-runtime-2.2.20-dev-7701.jar"),
//        File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-daemon-embeddable/2.2.20-dev-7701/10e49a3d08d38f2cb042bf6d37109dd5d79073d7/kotlin-daemon-embeddable-2.2.20-dev-7701.jar"),
//        File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/1.8.0/ac1dc37a30a93150b704022f8d895ee1bd3a36b3/kotlinx-coroutines-core-jvm-1.8.0.jar"),
//        File("/Users/michal.svec/.gradle/caches/modules-2/files-2.1/org.jetbrains/annotations/13.0/919f0dfe192fb4e063e7dacadee7f8bb9a2672a9/annotations-13.0.jar")
//    )


//    val compilerClassPath = getKotlinPaths().classPath(KotlinPaths.ClassPaths.Compiler)
//
//    val compilerWithScriptingClassPath = getKotlinPaths().classPath(KotlinPaths.ClassPaths.CompilerWithScripting)
//
//    val daemonClientClassPath = listOf(File(getCompilerLib(), "kotlin-daemon-client.jar"),
//                                       File(getCompilerLib(), "kotlin-compiler.jar"))

//    val compilerId by lazy(LazyThreadSafetyMode.NONE) { CompilerId.makeCompilerId(compilerClassPath) }
//    val compilerId = CompilerId.makeCompilerId(compilerFullClasspath)
    val compilerId = CompilerId()
    val daemonOptions = DaemonOptions()
    val daemonJVMOptions = configureDaemonJVMOptions(
        inheritMemoryLimits = true,
        inheritOtherJvmOptions = true,
        inheritAdditionalProperties = true
    )

    val timer = Timer(true)
    val (registry, port) = findPortAndCreateRegistry(COMPILE_DAEMON_FIND_PORT_ATTEMPTS, COMPILE_DAEMON_PORTS_RANGE_START, COMPILE_DAEMON_PORTS_RANGE_END)

    val compileService = CompileServiceImpl(
        registry = registry,
        compiler = compilerSelector,
        compilerId = compilerId,
        daemonOptions = daemonOptions,
        daemonJVMOptions = daemonJVMOptions,
        port = port,
        timer = timer,
        onShutdown = {
            if (daemonOptions.forceShutdownTimeoutMilliseconds != COMPILE_DAEMON_TIMEOUT_INFINITE_MS) {
                // running a watcher thread that ensures that if the daemon is not exited normally (may be due to RMI leftovers), it's forced to exit
                timer.schedule(daemonOptions.forceShutdownTimeoutMilliseconds) {
                    cancel()
                    println("force JVM shutdown")
                    exitProcess(0)
                }
            } else {
                timer.cancel()
            }
        })

    return compileService
}


fun main(){
    val cs = getCompileService()

    val sourceFiles = listOf(
        File("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/client/input/Input.kt"),
        File("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/client/input/Input2.kt"),
        File("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/client/input/Input3.kt")
    )
//    val compilerArguments =
//        sourceFiles.map { it -> it.absolutePath } + "-d" + buildAbsPath(OUTPUT_FILES_DIR) + "-cp" + "/Users/michal.svec/Desktop/jars/kotlin-stdlib-2.2.0.jar /Users/michal.svec/Desktop/jars/kotlin-reflect-2.2.0.jar"
//    println("DEBUG SERVER: compilerArguments=${compilerArguments.contentToString()}")


//    val compilerArguments = sourceFiles.map { it.absolutePath } +
//            listOf("-d", buildAbsPath(OUTPUT_FILES_DIR),
//                   "-cp", "/Users/michal.svec/Desktop/jars/kotlin-stdlib-2.2.0.jar:/Users/michal.svec/Desktop/jars/kotlin-reflect-2.2.0.jar",
//                   "-kotlin-home", "/path/to/kotlin/distribution",
//                   "-no-stdlib", "-no-reflect")
    val compilerArguments = sourceFiles.map { it.absolutePath } +
            listOf("-d", buildAbsPath(OUTPUT_FILES_DIR),
                   "-cp", "/Users/michal.svec/Desktop/jars/kotlin-scripting-compiler-2.2.0.jar:/Users/michal.svec/Desktop/jars/kotlin-stdlib-2.2.0.jar:/Users/michal.svec/Desktop/jars/kotlin-reflect-2.2.0.jar:/Users/michal.svec/Desktop/jars/kotlin-script-runtime-2.2.0.jar")

    println("compiler arguments are $compilerArguments")
    val remoteMessageCollector = RemoteMessageCollector(object : MessageSender {
        override fun send(msg: MessageCollectorImpl.Message) {
            println("MESSAGE COLLECTOR: $msg\n")
        }
    })

    val outputsCollector = { x: File, y: List<File> -> println("$x $y") }
    val servicesFacade = BasicCompilerServicesWithResultsFacadeServer(remoteMessageCollector, outputsCollector)

    val compilationOptions = CompilationOptions(
        compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
        targetPlatform = CompileService.TargetPlatform.JVM,
        reportSeverity = ReportSeverity.DEBUG.code,
        reportCategories = arrayOf(),
        requestedCompilationResults = arrayOf(),
    )
    try {

//        val result = cs.getDaemonInfo()
        val session = cs.leaseCompileSession("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/client/KotlinDaemonClient.kt")
        val sessionId = session.get()
        println("session id is $sessionId")

        val result = cs.compile(
            sessionId = sessionId,
            compilerArguments = compilerArguments.toTypedArray(),
            compilationOptions = compilationOptions,
            servicesFacade = servicesFacade,
            compilationResults = null
        )
        print("compilation result is $result")
    }catch (e: Exception){
        println("this is my exception $e")
    }
}