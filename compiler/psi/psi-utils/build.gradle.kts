import org.jetbrains.kotlin.build.foreign.CheckForeignClassUsageTask

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

    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
    testFixturesCompileOnly(intellijCore())
    testCompileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)

    /** The 'test' task inputs cannot depend on [checkForeignClassUsage] outputs. */
    testData(project.isolated, "api/psi-utils-api.api")
    testData(project.isolated, "api/psi-utils-api.undocumented")

    testData(project.isolated, "src")
}

private val stableNonPublicMarkers = listOf(
    "org.jetbrains.kotlin.psi.KtImplementationDetail",
)

val checkForeignClassUsage by tasks.registering(CheckForeignClassUsageTask::class) {
    outputFile = file("api/psi-utils-api.foreign")
    nonPublicMarkers.addAll(stableNonPublicMarkers)
}