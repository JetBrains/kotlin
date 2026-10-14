import kotlinx.benchmark.gradle.benchmark

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    alias(libs.plugins.kotlinx.benchmark)
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(project(":compiler:incremental-compilation-impl"))
    testImplementation(intellijCore())
    testImplementation(libs.kotlinx.benchmark.runtime)
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

benchmark {
    configurations {
        // The plugin pre-registers `main`, which would give a `testBenchmark` task that runs every
        // benchmark in the module under JMH settings that do not suit them.
        // Drop it and register each suite explicitly, so every benchmark belongs to exactly one
        // task and neither suite needs to exclude the other.
        remove(getByName("main"))

        register("classpathSnapshot") {
            iterationTime = 1
            iterationTimeUnit = "sec"
            warmups = 10
            iterations = 30
        }
    }
    targets {
        register("test")
    }
}

tasks.withType<JavaExec>().configureEach {
    addJarPathProperty("classpathSnapshot.stdlib", project.dependencies.project(":kotlin-stdlib"))
    addJarPathProperty("classpathSnapshot.gradleApi", project.dependencies.gradleApi())
}
