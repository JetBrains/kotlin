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

val warmupsParam = providers.gradleProperty("warmups").orNull
val iterationsParam = providers.gradleProperty("iterations").orNull
val includePattern = providers.gradleProperty("include").orNull
val sizeParam = providers.gradleProperty("size").orNull

val reflectionInclude = "org.jetbrains.kotlin.benchmarks.jmh.jvm.reflection.*"

// The reflection benchmark variants share the same JMH setup and differ only in the reflection
// implementation, which is selected by the system properties wired onto their exec tasks below.
val reflectionBenchmarks = mapOf(
    "reflection" to mapOf(
        "kotlin.reflect.jvm.useK1Implementation" to "false",
        "kotlin.reflect.jvm.newFakeOverridesImplementation" to "true",
    ),
    "reflectionK1" to mapOf(
        "kotlin.reflect.jvm.useK1Implementation" to "true",
    ),
)

// kotlin-reflect built from this working tree. The reflection benchmarks must measure local changes to
// it, but nothing else in this module does, so it is injected into their exec tasks' classpath below
// instead of being declared as a `testImplementation` dependency. That keeps `:kotlin-reflect` out of
// the task graph of `testBenchmark` and of this module's tests.
val localKotlinReflectDeclaration = configurations.dependencyScopeNamedOrRegister("localKotlinReflect") {
    dependencies.add(project.dependencies.project(":kotlin-reflect"))
}
val localKotlinReflect = configurations.resolvableNamedOrRegister("localKotlinReflectClasspath") {
    extendsFrom(localKotlinReflectDeclaration.get())
    isTransitive = false
}

benchmark {
    configurations {
        for (configurationName in reflectionBenchmarks.keys) {
            register(configurationName) {
                include(reflectionInclude)

                iterationTime = 1 // Required param
                iterationTimeUnit = "sec" // Required param

                warmups = warmupsParam?.toInt() ?: 0
                iterations = iterationsParam?.toInt() ?: 1
                advanced("jvmForks", 50)

                if (sizeParam != null) {
                    param("size", sizeParam.toInt())
                }
            }
        }

        named("main") {
            iterationTime = 1 // Required param
            iterationTimeUnit = "sec" // Required param

            warmups = warmupsParam?.toInt() ?: 5 // `5` is currently default in JMH
            iterations = iterationsParam?.toInt() ?: 5 // `5` is currently default in JMH

            include(includePattern ?: "*") // Benchmark everything if the pattern isn't specified
            exclude(reflectionInclude)

            if (sizeParam != null) {
                // Use size from annotation arguments if the param isn't specified
                // CAUTION: large size might cause long execution time
                param("size", sizeParam.toInt())
            }
        }
    }
    targets {
        register("test")
    }
}

tasks.withType<JavaExec>().matching { it.name == "testBenchmark" }.configureEach {
    val ideaHomeForTests = this.project.configurations.detachedConfiguration(this.project.dependencies.project(":", configuration = "ideaHomeForTests"))
    jvmArgumentProviders.add(this.project.objects.newInstance(SystemPropertyClasspathDirectoryProvider::class.java).apply {
        property.set("idea.home.path")
        classpath.from(ideaHomeForTests)
        directory.value(ideaHomePathForTests())
    })

    systemProperty("idea.use.native.fs.for.win", false)
}

// kotlinx-benchmark registers the benchmark exec tasks, and sets their classpath, from its own
// `afterEvaluate`. Configuring them from a later one is what makes the classpath rewrite below observe
// the classpath the plugin assembled instead of an empty one.
afterEvaluate {
    val reflectionExecTaskNames = reflectionBenchmarks.map { (configurationName, benchmarkSystemProperties) ->
        // kotlinx-benchmark names the exec task "<target><CapitalizedConfiguration>Benchmark"
        val taskName = "test${configurationName.replaceFirstChar { it.uppercaseChar() }}Benchmark"
        tasks.withType<JavaExec>().matching { it.name == taskName }.configureEach {
            benchmarkSystemProperties.forEach { (key, value) -> systemProperty(key, value) }

            // Swap the published kotlin-reflect inherited from the test runtime classpath for the local one
            classpath = files(localKotlinReflect) + classpath.filter { !it.name.startsWith("kotlin-reflect") }
        }
        taskName
    }

    // For debugging: `reflectionBenchmark` runs every reflection variant, each as its own exec task, so
    // the implementations are measured in separate JVMs and reported separately.
    tasks.named("reflectionBenchmark") {
        dependsOn(reflectionExecTaskNames)
    }

    // The variants must run strictly one after another. `org.gradle.parallel=true` would otherwise let
    // them run at once, which both distorts the measurements and makes them fight over JMH's global
    // lock file - the loser reports a failure but does not fail the build, silently losing its results.
    reflectionExecTaskNames.zipWithNext().forEach { (earlier, later) ->
        tasks.named(later) { mustRunAfter(earlier) }
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
