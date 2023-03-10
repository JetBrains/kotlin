import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Common klib reader and writer"

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-util-io"))
    testImplementation(commonDependency("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        languageVersion = "1.4"
        apiVersion = "1.4"
        freeCompilerArgs += listOf("-Xsuppress-version-warnings")
    }
}

publish()

standardPublicJars()