import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:light-classes-base"))
    implementation(intellijCore())

    compileOnly(libs.guava)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
}

private val stableNonPublicMarkers = listOf(
    "org.jetbrains.kotlin.psi.KtImplementationDetail",
    "org.jetbrains.kotlin.psi.KtNonPublicApi",
    "org.jetbrains.kotlin.psi.KtExperimentalApi",
)

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        filters {
            exclude.annotatedWith.addAll(stableNonPublicMarkers)
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

projectTests {
    testCodebaseTask()
}

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    outputFile = file("api/kt-references.foreign")
    nonPublicMarkers.addAll(stableNonPublicMarkers)
}
