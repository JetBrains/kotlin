import org.jetbrains.kotlin.gradle.dsl.KotlinVersion as GradleKotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

description = "Kotlin Build Report Common"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":kotlin-util-io"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    compileOnly(kotlinStdlib())
    compileOnly(intellijCore())
    implementation(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(commonDependency("com.google.code.gson:gson"))
    testApi(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5, parallel = true)
