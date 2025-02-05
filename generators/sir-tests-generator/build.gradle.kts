plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { java.srcDirs("main") }
}

dependencies {
    implementation(projectTests(":native:swift:sir-compiler-bridge"))
    implementation(project(":native:swift:swift-export-standalone-integration-tests"))
    implementation(projectTests(":native:swift:swift-export-ide"))
    implementation(projectTests(":generators:test-generator"))
    implementation(projectTests(":generators:analysis-api-generator"))
    implementation(projectTests(":analysis:analysis-test-framework"))
    implementation(projectTests(":analysis:analysis-api-impl-base"))

    implementation(projectTests(":analysis:analysis-api-fir"))

    runtimeOnly(libs.junit.jupiter.api)
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.native.swift.sir.GenerateSirTestsKt")
