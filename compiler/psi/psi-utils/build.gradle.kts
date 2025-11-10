import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask

plugins {
    kotlin("jvm")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend.common"))
    api(project(":kotlin-script-runtime"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)

    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

private val stableNonPublicMarkers = listOf(
    "org.jetbrains.kotlin.psi.KtImplementationDetail",
)

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    outputFile = file("api/psi-utils-api.foreign")
    nonPublicMarkers.addAll(stableNonPublicMarkers)
}