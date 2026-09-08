import org.jetbrains.kotlin.build.foreign.registerForeignClassUsageTasks

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
    id("java-test-fixtures")
    id("test-inputs-check")
}

dependencies {

    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)

    implementation(project(":compiler:psi:psi-api"))
    runtimeOnly(project(":compiler:psi:psi-impl"))

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
    "test" { none() }
}

projectTests {
    testCodebaseTask()
}

private val stableNonPublicMarkers = listOf(
    "org.jetbrains.kotlin.psi.KtImplementationDetail",
)

registerForeignClassUsageTasks {
    outputFile = file("api/psi-utils-api.foreign")
    nonPublicMarkers.addAll(stableNonPublicMarkers)
}
