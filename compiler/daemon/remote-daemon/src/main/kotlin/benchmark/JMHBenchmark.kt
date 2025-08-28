package benchmark

import common.RemoteCompilationServiceImplType
import kotlinx.coroutines.runBlocking
import main.kotlin.server.RemoteCompilationServer
import org.jetbrains.kotlin.client.RemoteCompilationClient
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.openjdk.jmh.annotations.Benchmark as JmhBenchmark
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi

@State(Scope.Benchmark)
open class JMHBenchmark(
    private val serverImplType: RemoteCompilationServiceImplType = RemoteCompilationServiceImplType.GRPC
) {

    private lateinit var ktorTasks: List<Task>
    private lateinit var client: RemoteCompilationClient
    private lateinit var compilationOptions: CompilationOptions
    private lateinit var server: RemoteCompilationServer

    @Setup
    fun setup() {
        server = RemoteCompilationServer(50051, serverImplType)
        server.start()

        client = RemoteCompilationClient(serverImplType)
        // TODO: adjust path/data source as needed
        ktorTasks = TasksExtractor
            .getTasks("/Users/michal.svec/Desktop/kotlin/compiler/daemon/remote-daemon/src/main/kotlin/benchmark/compileOutput")
            .subList(0, 10) // TODO adjust size

        compilationOptions = CompilationOptions(
            compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
            targetPlatform = CompileService.TargetPlatform.JVM,
            reportSeverity = 0,
            reportCategories = arrayOf(),
            requestedCompilationResults = arrayOf(),
        )
    }

    @OptIn(ExperimentalPathApi::class)
    @TearDown(Level.Iteration)
    fun iterationTearDown() {
        println("TEAR DOWN CALLED")
        server.cleanup()
    }

    @TearDown(Level.Trial)
    fun serverTearDown() {
        println("SERVER TEAR DOWN CALLED")
        server.stop()
    }

    // to do the setup steps described in Benchmark.kt file,
    // you can run this JMH benchmark using the following command
    // ./gradlew jmh -DjmhIncludes="benchmark.JMHBenchmark"
    @JmhBenchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
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