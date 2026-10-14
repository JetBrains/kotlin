import kotlinx.benchmark.gradle.benchmark

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    alias(libs.plugins.kotlinx.benchmark)
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(project(":compiler:cli"))
    testImplementation(intellijCore())
    testImplementation(libs.kotlinx.benchmark.runtime)
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

val compilationWarmupsParam = providers.gradleProperty("compilationWarmups").orNull
val compilationIterationsParam = providers.gradleProperty("compilationIterations").orNull
val compilationIncludeParam = providers.gradleProperty("compilationInclude").orNull
val compilationSizeParam = providers.gradleProperty("compilationSize").orNull

benchmark {
    configurations {
        // The plugin pre-registers `main`, which would give a `testBenchmark` task that runs every
        // benchmark in the module under JMH settings that do not suit them.
        // Drop it and register each suite explicitly, so every benchmark belongs to exactly one
        // task and neither suite needs to exclude the other.
        remove(getByName("main"))

        register("compilation") {
            compilationIncludeParam?.let { include(it) }

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
    }
    targets {
        register("test")
    }
}

tasks.withType<JavaExec>().configureEach {
    val ideaHomeForTests = project.configurations
        .detachedConfiguration(project.dependencies.project(":", configuration = "ideaHomeForTests"))
    jvmArgumentProviders.add(project.objects.newInstance(SystemPropertyClasspathDirectoryProvider::class.java).apply {
        property.set("idea.home.path")
        classpath.from(ideaHomeForTests)
        directory.value(ideaHomePathForTests())
    })

    systemProperty("idea.use.native.fs.for.win", false)
}
