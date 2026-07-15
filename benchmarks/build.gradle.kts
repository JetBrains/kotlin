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

val warmupsParam = project.findProperty("warmups")?.toString()
val iterationsParam = project.findProperty("iterations")?.toString()
val includePattern = project.findProperty("include")?.toString()
val sizeParam = project.findProperty("size")?.toString()

benchmark {
    configurations {
        named("main") {
            iterationTime = 1 // Required param
            iterationTimeUnit = "sec" // Required param

            warmups = warmupsParam?.toInt() ?: 5 // `5` is currently default in JMH
            iterations = iterationsParam?.toInt() ?: 5 // `5` is currently default in JMH

            include(includePattern ?: "*") // Benchmark everything if the pattern isn't specified

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

tasks.withType<JmhBytecodeGeneratorTask>().configureEach {
    outputs.cacheIf("Disabled because of https://github.com/Kotlin/kotlinx-benchmark/issues/364 (remove after version upgrading)") {
        false
    }
}

projectTests {
    testTask {
        workingDir = rootDir
    }

    withJvmStdlibAndReflect()
}
