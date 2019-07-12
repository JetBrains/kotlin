package org.jetbrains.kotlin.benchmark

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.HostManager
import javax.inject.Inject

private val NamedDomainObjectContainer<KotlinSourceSet>.commonMain
    get() = maybeCreate("commonMain")

private val NamedDomainObjectContainer<KotlinSourceSet>.nativeMain
    get() = maybeCreate("nativeMain")

private val NamedDomainObjectContainer<KotlinSourceSet>.jvmMain
    get() = maybeCreate("jvmMain")

private val Project.benchmark: BenchmarkExtension
    get() = extensions.getByName(BenchmarkingPlugin.BENCHMARK_EXTENSION_NAME) as BenchmarkExtension

private val Project.nativeWarmup: Int
    get() = (property("nativeWarmup") as String).toInt()

private val Project.jvmWarmup: Int
    get() = (property("jvmWarmup") as String).toInt()

private val Project.attempts: Int
    get() = (property("attempts") as String).toInt()

private val Project.nativeBenchResults: String
    get() = property("nativeBenchResults") as String

private val Project.jvmBenchResults: String
    get() = property("jvmBenchResults") as String

private val Project.compilerArgs: List<String>
    get() = (findProperty("compilerArgs") as String?)?.split("\\s").orEmpty()

internal val Project.kotlinVersion: String
    get() = property("kotlinVersion") as String

internal val Project.konanVersion: String
    get() = property("konanVersion") as String

internal val Project.kotlinStdlibVersion: String
    get() = property("kotlinStdlibVersion") as String

internal val Project.kotlinStdlibRepo: String
    get() = property("kotlinStdlibRepo") as String

internal val Project.nativeJson: String
    get() = project.property("nativeJson") as String

internal val Project.jvmJson: String
    get() = project.property("jvmJson") as String

internal val Project.commonBenchmarkProperties: Map<String, Any>
    get() = mapOf(
        "cpu" to System.getProperty("os.arch"),
        "os" to System.getProperty("os.name"),
        "jdkVersion" to System.getProperty("java.version"),
        "jdkVendor" to System.getProperty("java.vendor"),
        "kotlinVersion" to kotlinVersion
    )

open class BenchmarkExtension @Inject constructor(val project: Project) {
    var applicationName: String = project.name
    var commonSrcDirs: Collection<Any> = emptyList()
    var jvmSrcDirs: Collection<Any> = emptyList()
    var nativeSrcDirs: Collection<Any> = emptyList()
    var linkerOpts: Collection<String> = emptyList()
}

/**
 * A plugin configuring a benchmark Kotlin/Native project.
 */
open class BenchmarkingPlugin: Plugin<Project> {

    private val mingwPath: String = System.getenv("MINGW64_DIR") ?: "c:/msys64/mingw64"

    private fun Project.determinePreset(): KotlinNativeTargetPreset =
        defaultHostPreset(this).also { preset ->
            logger.quiet("$project has been configured for ${preset.name} platform.")
        } as KotlinNativeTargetPreset

    private fun Project.configureSourceSets(kotlinVersion: String) {
        with(kotlin.sourceSets) {
            commonMain.dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinStdlibVersion")
            }

            jvmMain.dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinStdlibVersion")
            }

            repositories.maven {
                it.setUrl(kotlinStdlibRepo)
            }

            // Add sources specified by a user in the benchmark DSL.
            afterEvaluate {
                benchmark.let {
                    commonMain.kotlin.srcDirs(*it.commonSrcDirs.toTypedArray())
                    nativeMain.kotlin.srcDirs(*it.nativeSrcDirs.toTypedArray())
                    jvmMain.kotlin.srcDirs(*it.jvmSrcDirs.toTypedArray())
                }
            }
        }
    }

    private fun Project.configureJVMTarget() {
        kotlin.jvm {
            compilations.all {
                it.compileKotlinTask.kotlinOptions {
                    jvmTarget = "1.8"
                    suppressWarnings = true
                    freeCompilerArgs = project.compilerArgs
                }
            }
        }
    }

    private fun Project.configureNativeTarget(hostPreset: KotlinNativeTargetPreset) {
        kotlin.targetFromPreset(hostPreset, NATIVE_TARGET_NAME) {
            compilations.getByName("main").kotlinOptions.freeCompilerArgs = project.compilerArgs + "-opt"
            binaries.executable(NATIVE_EXECUTABLE_NAME, listOf(RELEASE)) {
                if (HostManager.hostIsMingw) {
                    linkerOpts.add("-L${mingwPath}/lib")
                }

                runTask!!.apply {
                    group = ""
                    enabled = false
                }

                // Specify settings configured by a user in the benchmark extension.
                afterEvaluate {
                    linkerOpts.addAll(benchmark.linkerOpts)
                }
            }
        }
    }


    private fun Project.configureMPPExtension() {
        configureSourceSets(kotlinVersion)
        configureJVMTarget()
        configureNativeTarget(determinePreset())
    }


    private fun Project.configureTasks() {
        // Native run task.
        val nativeTarget = kotlin.targets.getByName(NATIVE_TARGET_NAME) as KotlinNativeTarget
        val nativeExecutable = nativeTarget.binaries.getExecutable(NATIVE_EXECUTABLE_NAME, NativeBuildType.RELEASE)
        val konanRun = createRunTask(this, "konanRun", nativeExecutable.linkTask,
                buildDir.resolve(nativeBenchResults).absolutePath).apply {
            group = BENCHMARKING_GROUP
            description = "Runs the benchmark for Kotlin/Native."
        }
        afterEvaluate {
            (konanRun as RunKotlinNativeTask).args(
                    "-w", nativeWarmup.toString(),
                    "-r", attempts.toString(),
                    "-p", "${benchmark.applicationName}::"
            )
        }

        // JVM run task.
        val jvmRun = tasks.create("jvmRun", RunJvmTask::class.java) { task ->
            task.dependsOn("build")
            val mainCompilation = kotlin.jvm().compilations.getByName("main")
            val runtimeDependencies = configurations.getByName(mainCompilation.runtimeDependencyConfigurationName)
            task.classpath(files(mainCompilation.output.allOutputs, runtimeDependencies))
            task.main = "MainKt"

            task.group = BENCHMARKING_GROUP
            task.description = "Runs the benchmark for Kotlin/JVM."

            // Specify settings configured by a user in the benchmark extension.
            afterEvaluate {
                task.args(
                    "-w", jvmWarmup,
                    "-r", attempts,
                    "-o", buildDir.resolve(jvmBenchResults),
                    "-p", "${benchmark.applicationName}::"
                )
            }
        }

        // Native report task.
        val konanJsonReport = tasks.create("konanJsonReport") {

            it.group = BENCHMARKING_GROUP
            it.description = "Builds the benchmarking report for Kotlin/Native."

            it.doLast {
                val applicationName = benchmark.applicationName
                val nativeCompileTime = getNativeCompileTime(applicationName)
                val benchContents = buildDir.resolve(nativeBenchResults).readText()

                val properties = commonBenchmarkProperties + mapOf(
                    "type" to "native",
                    "compilerVersion" to konanVersion,
                    "flags" to nativeTarget.compilations.main.kotlinOptions.freeCompilerArgs.map { "\"$it\"" },
                    "benchmarks" to benchContents,
                    "compileTime" to listOf(nativeCompileTime),
                    "codeSize" to getCodeSizeBenchmark(applicationName, nativeExecutable.outputFile.absolutePath)
                )

                val output = createJsonReport(properties)
                buildDir.resolve(nativeJson).writeText(output)
            }
        }

        // JVM report task.
        val jvmJsonReport = tasks.create("jvmJsonReport") {

            it.group = BENCHMARKING_GROUP
            it.description = "Builds the benchmarking report for Kotlin/JVM."

            it.doLast {
                val applicationName = benchmark.applicationName
                val jarPath = (tasks.getByName("jvmJar") as Jar).archiveFile.get().asFile
                val jvmCompileTime = getJvmCompileTime(applicationName)
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

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("kotlin-multiplatform")

        // Use Kotlin compiler version specified by the project property.
        dependencies.add("kotlinCompilerClasspath", "org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")

        extensions.create(BENCHMARK_EXTENSION_NAME, BenchmarkExtension::class.java, this)
        configureMPPExtension()
        addTimeListener(this)
        configureTasks()
    }

    companion object {
        const val NATIVE_TARGET_NAME = "native"
        const val NATIVE_EXECUTABLE_NAME = "benchmark"
        const val BENCHMARK_EXTENSION_NAME = "benchmark"

        const val BENCHMARKING_GROUP = "benchmarking"
    }
}
