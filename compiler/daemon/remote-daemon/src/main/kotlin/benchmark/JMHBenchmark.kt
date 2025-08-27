package benchmark

import common.CLIENT_COMPILED_DIR
import common.CLIENT_TMP_DIR
import common.RemoteCompilationServiceImplType
import common.SERVER_ARTIFACTS_CACHE_DIR
import common.SERVER_COMPILATION_WORKSPACE_DIR
import common.SERVER_TMP_CACHE_DIR
import kotlinx.coroutines.runBlocking
import main.kotlin.server.RemoteCompilationServer
import org.jetbrains.kotlin.client.RemoteCompilationClient
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.openjdk.jmh.annotations.Benchmark as JmhBenchmark
import org.openjdk.jmh.annotations.*
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@State(Scope.Benchmark)
open class JMHBenchmark(
    private val serverImplType: RemoteCompilationServiceImplType = RemoteCompilationServiceImplType.GRPC
) {

    private lateinit var ktorTasks: List<Task>
    private lateinit var client: RemoteCompilationClient
    private lateinit var compilationOptions: CompilationOptions

    @Setup
    fun setup() {
        RemoteCompilationServer(50051, serverImplType).start()
        client = RemoteCompilationClient(serverImplType)
        // TODO: adjust path/data source as needed
        ktorTasks = DataExtractor
            .getTask("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/benchmark/compileOutput3")

        compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
            reportSeverity = 0,
            reportCategories = arrayOf(),
            requestedCompilationResults = arrayOf(),
        )
    }

    @OptIn(ExperimentalPathApi::class)
    @TearDown
    fun tearDown() {
        println("TEAR DOWN CALLED")
        CLIENT_COMPILED_DIR.deleteRecursively()
        CLIENT_TMP_DIR.deleteRecursively()
        SERVER_TMP_CACHE_DIR.deleteRecursively()
        SERVER_ARTIFACTS_CACHE_DIR.deleteRecursively()
        SERVER_COMPILATION_WORKSPACE_DIR.deleteRecursively()

        Files.createDirectories(CLIENT_COMPILED_DIR)
        Files.createDirectories(CLIENT_TMP_DIR)
        Files.createDirectories(SERVER_TMP_CACHE_DIR)
        Files.createDirectories(SERVER_ARTIFACTS_CACHE_DIR)
        Files.createDirectories(SERVER_COMPILATION_WORKSPACE_DIR)

    }

    @JmhBenchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 1, time = 1)
    @Measurement(iterations = 5, time = 1)
    @Fork(1)
    @Threads(1)
    fun compileProject() = runBlocking {
        ktorTasks.forEachIndexed { index, task ->
            val res = client.compile(
                "ktor-task-$index",
                task.compilerArgs,
                compilationOptions,
            )
            println("Compilation number $index finished with exit code ${res.exitCode}")
            require(res.exitCode == 0) { "Compilation number $index failed with exit code ${res.exitCode}" }
        }
    }
}