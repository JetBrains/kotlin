import org.jetbrains.kotlin.gradle.dsl.Coroutines

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":kotlin-stdlib"))
    compileOnly(project(":kotlin-reflect-api"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    runtimeOnly(project(":kotlin-reflect"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}