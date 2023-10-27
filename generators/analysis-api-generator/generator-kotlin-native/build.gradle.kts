plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

dependencies {
    api(kotlinStdlib("jdk8"))

    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-spec"))
    testImplementation(projectTests(":generators:analysis-api-generator"))

    testImplementation(projectTests(":analysis:low-level-api-fir"))
    testImplementation(projectTests(":analysis:analysis-api-impl-barebone"))
    testImplementation(projectTests(":analysis:low-level-api-fir:low-level-api-fir-native"))

    testImplementation(intellijCore())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}


val generateAnalysisApiNativeTests by generator("org.jetbrains.kotlin.generators.tests.analysis.api.konan.GenerateAnalysisApiNativeTestsKt")

testsJar()

