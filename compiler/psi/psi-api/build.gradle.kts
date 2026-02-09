import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend.common"))
    api(project(":kotlin-script-runtime"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesCompileOnly(intellijCore())
    testCompileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
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

testsJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)

    /** The 'test' task inputs cannot depend on [checkForeignClassUsage] outputs. */
    testData(project.isolated, "api/psi-api.api")
    testData(project.isolated, "api/psi-api.undocumented")

    testData(project.isolated, "src")
    testData(project(":compiler:psi:psi-impl").isolated, "src")
    testData(project(":compiler:psi:psi-utils").isolated, "src")
    testData(project(":compiler:psi:psi-frontend-utils").isolated, "src")
}

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    outputFile = file("api/psi-api.foreign")
    nonPublicMarkers.addAll(stableNonPublicMarkers)
}

tasks.named("checkKotlinAbi").configure {
    /**
     * The ABI task depends on the whole 'api/' directory which contains output of the [checkForeignClassUsage] task.
     * Gradle requires having an explicit task dependency when inputs of a certain task contain outputs of another.
     */
    dependsOn(checkForeignClassUsage)
}