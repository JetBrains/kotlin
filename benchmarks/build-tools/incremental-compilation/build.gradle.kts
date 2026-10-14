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
        named("main") {
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
