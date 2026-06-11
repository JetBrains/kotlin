import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask

plugins {
    kotlin("jvm")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    compileOnly(project(":kotlin-stdlib"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)

    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

projectTests {
    testCodebaseTask()
}

private val stableNonPublicMarkers = listOf(
    "org.jetbrains.kotlin.psi.KtImplementationDetail",
)

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    outputFile = file("api/psi-utils-api.foreign")
    nonPublicMarkers.addAll(stableNonPublicMarkers)
}
