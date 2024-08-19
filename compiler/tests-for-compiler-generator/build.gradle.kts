plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(kotlinStdlib())

    testApi(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler"))
    testImplementation(projectTests(":compiler:fir:raw-fir:psi2fir"))
    testImplementation(projectTests(":compiler:fir:raw-fir:light-tree2fir"))
    testImplementation(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(projectTests(":compiler:fir:analysis-tests"))

    testImplementation(project(":compiler:backend"))
    testImplementation(project(":compiler:backend.common.jvm"))
    testImplementation(project(":compiler:test-infrastructure-utils"))
    testImplementation(project(":compiler:tests-compiler-utils"))
    testImplementation(project(":libraries:tools:abi-comparator"))
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

val generateTests by generator("org.jetbrains.kotlin.test.generators.GenerateCompilerTestsKt") {
    dependsOn(":compiler:generateTestData")
}

testsJar()
