plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

dependencies {
    api(kotlinStdlib("jdk8"))

    testImplementation(testFixtures(project(":generators:test-generator")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-spec")))
    testImplementation(testFixtures(project(":generators:analysis-api-generator")))

    testImplementation(testFixtures(project(":analysis:low-level-api-fir")))
    testImplementation(testFixtures(project(":analysis:low-level-api-fir:low-level-api-fir-native")))

    testImplementation(intellijCore())
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(testFixtures(project(":analysis:analysis-test-framework")))
}


val generateAnalysisApiNativeTests by generator("org.jetbrains.kotlin.generators.tests.analysis.api.konan.GenNativeTestsKt", testSourceSet)

testsJar()

