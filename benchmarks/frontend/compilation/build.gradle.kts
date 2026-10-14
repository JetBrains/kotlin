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

benchmark {
    configurations {
        named("main") {
            providers.gradleProperty("include").orNull?.let { include(it) }

            iterationTime = 1 // Required param
            iterationTimeUnit = "sec" // Required param

            warmups = providers.gradleProperty("warmups").orNull?.toInt() ?: 5 // `5` is currently default in JMH
            iterations = providers.gradleProperty("iterations").orNull?.toInt() ?: 5 // `5` is currently default in JMH

            // Use size from annotation arguments if the param isn't specified.
            // `size` is the name of the benchmarks' own `@Param` property, so it stays as it is.
            // CAUTION: large size might cause long execution time
            providers.gradleProperty("size").orNull?.let { param("size", it.toInt()) }
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
