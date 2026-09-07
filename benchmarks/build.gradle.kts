import kotlinx.benchmark.gradle.JmhBytecodeGeneratorTask
import kotlinx.benchmark.gradle.benchmark

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    alias(libs.plugins.kotlinx.benchmark)
    id("project-tests-convention")
    id("java-test-fixtures")
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(project(":compiler:cli"))
    testImplementation(intellijCore())
    testImplementation(libs.kotlinx.benchmark.runtime)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testFixturesApi(testFixtures(project(":compiler:tests-integration")))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

val compilationWarmupsParam = providers.gradleProperty("compilationWarmups").orNull
val compilationIterationsParam = providers.gradleProperty("compilationIterations").orNull
val compilationIncludeParam = providers.gradleProperty("compilationInclude").orNull
val compilationSizeParam = providers.gradleProperty("compilationSize").orNull

val compilationBenchmarks = "org.jetbrains.kotlin.benchmarks.jmh.compilation.*"
val reflectionBenchmarks = "org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection.*"

benchmark {
    configurations {
        // The plugin pre-registers `main`, which would give a `testBenchmark` task that runs every
        // benchmark in the module under JMH settings that do not suit them.
        // Drop it and register each suite explicitly, so every benchmark belongs to exactly one
        // task and neither suite needs to exclude the other.
        remove(getByName("main"))

        // The compiler frontend benchmarks: `org.jetbrains.kotlin.benchmarks.jmh.compilation.*`.
        register("compilation") {
            include(compilationIncludeParam ?: compilationBenchmarks)

            iterationTime = 1 // Required param
            iterationTimeUnit = "sec" // Required param

            warmups = compilationWarmupsParam?.toInt() ?: 5 // `5` is currently default in JMH
            iterations = compilationIterationsParam?.toInt() ?: 5 // `5` is currently default in JMH

            if (compilationSizeParam != null) {
                // Use size from annotation arguments if the param isn't specified.
                // `size` is the name of the benchmarks' own `@Param` property, so it stays as it is.
                // CAUTION: large size might cause long execution time
                param("size", compilationSizeParam.toInt())
            }
        }

        // The reflection benchmarks carry their own JMH settings: they are single-shot and select the
        // reflection implementation through a `reflectImplementation` param
        register("reflection") {
            include(reflectionBenchmarks)

            iterationTime = 1 // Required param
            iterationTimeUnit = "sec" // Required param

            warmups = 0
            iterations = 1
            advanced("jvmForks", 30)
        }
    }
    targets {
        register("test")
    }
}

/**
 * Hands the jar built by [projectPath] to the benchmark JVM as `-D[systemProperty]=<path>`. The jar is a
 * tracked input of the task, so it gets built beforehand and the task re-runs when it changes.
 */
fun JavaExec.addJarPathProperty(systemProperty: String, projectPath: String) {
    val jar = project.configurations
        .detachedConfiguration(project.dependencies.project(projectPath))
        .apply { isTransitive = false }
    jvmArgumentProviders.add(project.objects.newInstance(SystemPropertyClasspathProvider::class.java).apply {
        property.set(systemProperty)
        classpath.from(jar)
    })
}

// kotlinx-benchmark registers the benchmark exec tasks from its own `afterEvaluate`, so they can only be
// looked up from a later one. Each task declares just the jars it uses, so running the compilation
// benchmarks does not build kotlin-reflect, and vice versa.
afterEvaluate {
    tasks.named<JavaExec>("testCompilationBenchmark") {
        val ideaHomeForTests = project.configurations
            .detachedConfiguration(project.dependencies.project(":", configuration = "ideaHomeForTests"))
        jvmArgumentProviders.add(project.objects.newInstance(SystemPropertyClasspathDirectoryProvider::class.java).apply {
            property.set("idea.home.path")
            classpath.from(ideaHomeForTests)
            directory.value(ideaHomePathForTests())
        })

        systemProperty("idea.use.native.fs.for.win", false)
    }

    // The reflection benchmarks measure kotlin-reflect built from this working tree. They get it the same
    // way the compiler tests do: as jar paths, which `StandardLibrariesPathProviderForKotlinProject` loads
    // into a dedicated class loader that also selects the K1 or the new implementation. Nothing of
    // kotlin-reflect goes on the benchmark's own classpath.
    tasks.named<JavaExec>("testReflectionBenchmark") {
        addJarPathProperty(TestCompilePaths.KOTLIN_FULL_STDLIB_PATH, ":kotlin-stdlib")
        addJarPathProperty(TestCompilePaths.KOTLIN_REFLECT_JAR_PATH, ":kotlin-reflect")
        // The benchmarks never touch these two, but `getOrCreateClassLoader` resolves all four jars
        // unconditionally and fails on a missing property. They are built for this module anyway.
        addJarPathProperty(TestCompilePaths.KOTLIN_SCRIPT_RUNTIME_PATH, ":kotlin-script-runtime")
        addJarPathProperty(TestCompilePaths.KOTLIN_TEST_JAR_PATH, ":kotlin-test")
    }
}

tasks.withType<JmhBytecodeGeneratorTask>().configureEach {
    outputs.cacheIf("Disabled because of https://github.com/Kotlin/kotlinx-benchmark/issues/364 (remove after version upgrading)") {
        false
    }
}

projectTests {
    testTask {
        workingDir = rootDir
    }
    // works for tasks.withType(Test::class.java) only and benchmarks are task<JavaExec>(...)
    withJvmStdlibAndReflect()
}
