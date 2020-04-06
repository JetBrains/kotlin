package org.jetbrains.kotlin.benchmark

import org.gradle.jvm.tasks.Jar
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.Executable
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import javax.inject.Inject
import kotlin.reflect.KClass

private val NamedDomainObjectContainer<KotlinSourceSet>.jvmMain
    get() = maybeCreate("jvmMain")

private val Project.jvmWarmup: Int
    get() = (property("jvmWarmup") as String).toInt()

private val Project.jvmBenchResults: String
    get() = property("jvmBenchResults") as String

open class KotlinNativeBenchmarkExtension @Inject constructor(project: Project) : BenchmarkExtension(project) {
    var jvmSrcDirs: Collection<Any> = emptyList()
    var mingwSrcDirs: Collection<Any> = emptyList()
    var posixSrcDirs: Collection<Any> = emptyList()

    fun BenchmarkExtension.BenchmarkDependencies.jvm(notation: Any) = sourceSets.jvmMain.dependencies {
            implementation(notation)
        }
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class KotlinNativeBenchmarkingPlugin: BenchmarkingPlugin() {
    override fun Project.configureJvmJsonTask(jvmRun: Task): Task {
        return tasks.create("jvmJsonReport") {
            it.group = BENCHMARKING_GROUP
            it.description = "Builds the benchmarking report for Kotlin/JVM."

            it.doLast {
                val applicationName = benchmark.applicationName
                val jarPath = (tasks.getByName("jvmJar") as Jar).archiveFile.get().asFile
                val jvmCompileTime = getJvmCompileTime(project, applicationName)
                val benchContents = buildDir.resolve(jvmBenchResults).readText()

                val properties: Map<String, Any> = commonBenchmarkProperties + mapOf(
                        "type" to "jvm",
                        "compilerVersion" to kotlinVersion,
                        "benchmarks" to benchContents,
                        "compileTime" to listOf(jvmCompileTime),
                        "codeSize" to getCodeSizeBenchmark(applicationName, jarPath.absolutePath)
                )

                val output = createJsonReport(properties)
                buildDir.resolve(jvmJson).writeText(output)
            }

            jvmRun.finalizedBy(it)
        }
    }

    override fun Project.configureJvmTask(): Task {
        return tasks.create("jvmRun", RunJvmTask::class.java) { task ->
            task.dependsOn("jvmJar")
            val mainCompilation = kotlin.jvm().compilations.getByName("main")
            val runtimeDependencies = configurations.getByName(mainCompilation.runtimeDependencyConfigurationName)
            task.classpath(files(mainCompilation.output.allOutputs, runtimeDependencies))
            task.main = "MainKt"

            task.group = BENCHMARKING_GROUP
            task.description = "Runs the benchmark for Kotlin/JVM."

            // Specify settings configured by a user in the benchmark extension.
            afterEvaluate {
                task.args("-p", "${benchmark.applicationName}::")
                task.warmupCount = jvmWarmup
                task.repeatCount = attempts
                task.outputFileName = buildDir.resolve(jvmBenchResults).absolutePath
                task.repeatingType = benchmark.repeatingType
            }
        }
    }

    override val benchmarkExtensionClass: KClass<*>
        get() = KotlinNativeBenchmarkExtension::class

    override val Project.benchmark: KotlinNativeBenchmarkExtension
        get() = extensions.getByName(benchmarkExtensionName) as KotlinNativeBenchmarkExtension

    override val benchmarkExtensionName: String = "benchmark"

    private val Project.nativeBinary: Executable
        get() = (kotlin.targets.getByName(NATIVE_TARGET_NAME) as KotlinNativeTarget)
            .binaries.getExecutable(NATIVE_EXECUTABLE_NAME, benchmark.buildType)

    override val Project.nativeExecutable: String
        get() = nativeBinary.outputFile.absolutePath

    override val Project.nativeLinkTask: Task
        get() = nativeBinary.linkTask

    override fun configureMPPExtension(project: Project) {
        super.configureMPPExtension(project)
        project.configureJVMTarget()
    }

    override fun getCompilerFlags(project: Project, nativeTarget: KotlinNativeTarget) =
            super.getCompilerFlags(project, nativeTarget) + project.nativeBinary.freeCompilerArgs.map { "\"$it\"" }

    override fun NamedDomainObjectContainer<KotlinSourceSet>.configureSources(project: Project) {
        project.benchmark.let {
            commonMain.kotlin.srcDirs(*it.commonSrcDirs.toTypedArray())
            if (HostManager.hostIsMingw) {
                nativeMain.kotlin.srcDirs(*(it.nativeSrcDirs + it.mingwSrcDirs).toTypedArray())
            } else {
                nativeMain.kotlin.srcDirs(*(it.nativeSrcDirs + it.posixSrcDirs).toTypedArray())
            }
            jvmMain.kotlin.srcDirs(*it.jvmSrcDirs.toTypedArray())
        }
    }

    override fun NamedDomainObjectContainer<KotlinSourceSet>.additionalConfigurations(project: Project) {
        jvmMain.dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${project.kotlinStdlibVersion}")
        }
    }

    private fun Project.configureJVMTarget() {
        kotlin.jvm {
            compilations.all {
                it.compileKotlinTask.kotlinOptions {
                    jvmTarget = "1.8"
                    suppressWarnings = true
                    freeCompilerArgs = project.benchmark.compilerOpts + project.compilerArgs
                }
            }
        }
    }

    companion object {
        const val BENCHMARK_EXTENSION_NAME = "benchmark"
    }
}
