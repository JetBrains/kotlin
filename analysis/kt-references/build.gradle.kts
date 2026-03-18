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
        enabled.set(true)
        filters {
            exclude.annotatedWith.addAll(stableNonPublicMarkers)
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
    "codebaseTest" {
        java.srcDirs("codebaseTest")
        compileClasspath += configurations["testCompileClasspath"]
        runtimeClasspath += configurations["testRuntimeClasspath"]
    }
}

projectTests {
    testTask(taskName = "testCodebase", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
        group = "verification"

        classpath += sourceSets.getByName("codebaseTest").runtimeClasspath
        testClassesDirs = sourceSets.getByName("codebaseTest").output.classesDirs
    }

    testData(project.isolated, "src")
    testData(project.isolated, "api")
}

tasks.named("check") {
    dependsOn("testCodebase")
}

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    outputFile = file("api/kt-references.foreign")
    nonPublicMarkers.addAll(stableNonPublicMarkers)
}

run /* Workaround for KT-84365 */ {
    tasks.named("checkKotlinAbi").configure {
        mustRunAfter(checkForeignClassUsage)
    }
    tasks.named("testCodebase").configure {
        mustRunAfter("updateKotlinAbi")
    }
    tasks.named("testCodebase").configure {
        mustRunAfter(checkForeignClassUsage)
    }
}
