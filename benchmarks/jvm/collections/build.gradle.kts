import kotlinx.benchmark.gradle.benchmark

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    alias(libs.plugins.kotlinx.benchmark)
}

dependencies {
    testImplementation(kotlinStdlib())
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

            iterationTime = 1
            iterationTimeUnit = "sec"
            warmups = providers.gradleProperty("warmups").orNull?.toInt() ?: 5
            iterations = providers.gradleProperty("iterations").orNull?.toInt() ?: 5
        }
    }
    targets {
        register("test")
    }
}
