package org.jetbrains.kotlin.benchmark

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.*
import javax.inject.Inject

private typealias CommandList = List<String>

open class CompileBenchmarkExtension @Inject constructor(val project: Project) {
    var applicationName = project.name
    var repeatNumber: Int = 1
    var buildSteps: Map<String, CommandList> = emptyMap()
}

open class CompileBenchmarkingPlugin : Plugin<Project> {

    private val exitCodes: MutableMap<String, Int> = mutableMapOf()
    
    private fun Project.configureUtilityTasks() {
        tasks.create("configureBuild") {
            it.doLast { mkdir(buildDir) }
        }

        tasks.create("clean", Delete::class.java) {
            it.delete(buildDir)
        }
    }
    
    private fun Project.configureKonanRun(
        benchmarkExtension: CompileBenchmarkExtension
    ): Unit = with(benchmarkExtension) {
        // Aggregate task.
        val konanRun = tasks.create("konanRun") { task ->
            task.dependsOn("configureBuild")
        }

        // Compile tasks.
        afterEvaluate {
            for (number in 1..repeatNumber) {
                buildSteps.forEach { (taskName, command) ->
                    tasks.create("$taskName$number", Exec::class.java).apply {
                        commandLine(command)
                        isIgnoreExitValue = true
                        konanRun.dependsOn(this)
                        doLast {
                            exitCodes[name] = execResult.exitValue
                        }
                    }
                }
            }
        }

        // Report task.
        tasks.create("konanJsonReport").apply {
            doLast {
                val nativeCompileTime = getCompileBenchmarkTime(
                    applicationName,
                    buildSteps.keys,
                    repeatNumber,
                    exitCodes
                )
                val nativeExecutable = buildDir.resolve("program${getNativeProgramExtension()}")
                val properties = commonBenchmarkProperties + mapOf(
                    "type" to "native",
                    "compilerVersion" to konanVersion,
                    "benchmarks" to "[]",
                    "compileTime" to nativeCompileTime,
                    "codeSize" to getCodeSizeBenchmark(applicationName, nativeExecutable.absolutePath)
                )
                val output = createJsonReport(properties)
                buildDir.resolve(nativeJson).writeText(output)
            }
            konanRun.finalizedBy(this)
        }
    }

    private fun Project.configureJvmRun(
        benchmarkExtension: CompileBenchmarkExtension
    ) {
        val jvmRun = tasks.create("jvmRun") {
            it.doLast { println("JVM run isn't supported") }
        }

        tasks.create("jvmJsonReport") {
            it.doLast { println("JVM run isn't supported") }
            jvmRun.finalizedBy(it)
        }
    }
    
    override fun apply(target: Project): Unit = with(target) {
        addTimeListener(this)

        val benchmarkExtension = extensions.create(
            COMPILE_BENCHMARK_EXTENSION_NAME,
            CompileBenchmarkExtension::class.java,
            this
        )

        // Create tasks.
        configureUtilityTasks()
        configureKonanRun(benchmarkExtension)
        configureJvmRun(benchmarkExtension)
    }

    companion object {
        const val COMPILE_BENCHMARK_EXTENSION_NAME = "compileBenchmark"
    }
}
