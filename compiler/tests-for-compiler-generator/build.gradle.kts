plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(kotlinStdlib())

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
    testImplementation(testFixtures(project(":compiler:tests-integration")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:fir:raw-fir:psi2fir")))
    testImplementation(testFixtures(project(":compiler:fir:raw-fir:light-tree2fir")))
    testImplementation(testFixtures(project(":compiler:fir:analysis-tests:legacy-fir-tests")))
    testImplementation(testFixtures(project(":js:js.tests")))
    testImplementation(testFixtures(project(":generators:test-generator")))
    testImplementation(testFixtures(project(":plugins:plugins-interactions-testing")))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

val generateTests by generator("org.jetbrains.kotlin.test.generators.GenerateCompilerTestsKt") {
    dependsOn(":compiler:generateTestData")
}

testsJar()
