import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

description = "Kotlin Daemon Client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val nativePlatformVariants = listOf(
    "windows-amd64",
    "windows-i386",
    "osx-amd64",
    "osx-i386",
    "linux-amd64",
    "linux-i386",
    "freebsd-amd64-libcpp",
    "freebsd-amd64-libstdcpp",
    "freebsd-i386-libcpp",
    "freebsd-i386-libstdcpp"
)

dependencies {
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":js:js.frontend"))
    compileOnly(commonDependency("net.rubygrapefruit", "native-platform"))

    embedded(project(":daemon-common")) { isTransitive = false }
    embedded(commonDependency("net.rubygrapefruit", "native-platform"))
    nativePlatformVariants.forEach {
        embedded(commonDependency("net.rubygrapefruit", "native-platform", "-$it"))
    }
    api(commonDependency("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) {
        isTransitive = false
    }
}

tasks.withType<KotlinCompilationTask<*>> {
    compilerOptions {
        // This module is being run from within Gradle, older versions of which only have older kotlin-stdlib in the runtime classpath.
        @Suppress("DEPRECATION")
        apiVersion.set(KotlinVersion.KOTLIN_1_4)
        @Suppress("DEPRECATION")
        languageVersion.set(KotlinVersion.KOTLIN_1_4)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

publish()

runtimeJar()

sourcesJar()

javadocJar()
