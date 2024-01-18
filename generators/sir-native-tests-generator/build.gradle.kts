plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { java.srcDirs("main") }
}

dependencies {
    implementation(projectTests(":kotlin-swift-export-compiler-plugin"))

    implementation(projectTests(":generators:test-generator"))
    runtimeOnly(projectTests(":analysis:analysis-test-framework"))
    runtimeOnly(libs.junit.jupiter.api)
}

val generateNativeTests by generator("org.jetbrains.kotlin.generators.tests.native.swift.sir.GenerateSirNativeTestsKt")