plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { java.srcDirs("main") }
}

dependencies {
    implementation(project(":native:swift:swift-export-standalone-integration-tests"))
    implementation(projectTests(":native:swift:swift-export-ide"))
    implementation(testFixtures(project(":generators:test-generator")))
    implementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    implementation(testFixtures(project(":analysis:analysis-test-framework")))
    implementation(testFixtures(project(":analysis:analysis-api-impl-base")))

    implementation(testFixtures(project(":analysis:analysis-api-fir")))

    runtimeOnly(libs.junit.jupiter.api)
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.native.swift.sir.GenerateSirTestsKt", mainSourceSet)
