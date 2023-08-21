import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

description = "Kotlin Daemon Client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":daemon-common"))
    compileOnly(libs.native.platform)

    embedded(project(":daemon-common")) { isTransitive = false }
    embedded(libs.native.platform)
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

runtimeJarWithRelocation {
    from(mainSourceSet.output)
    relocate("net.rubygrapefruit.platform", "$kotlinEmbeddableRootPackage.net.rubygrapefruit.platform") {
        exclude("net.rubygrapefruit.platform.internal.**") // cannot be relocated due to jni integration
    }
}
sourcesJar()
javadocJar()
