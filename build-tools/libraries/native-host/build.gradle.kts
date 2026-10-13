plugins {
    id("kotlin-build-tools-library")
}

description = "Kotlin Build Tools description of the Kotlin/Native build host"

dependencies {
    testImplementation(libs.junit.jupiter.params)
}
