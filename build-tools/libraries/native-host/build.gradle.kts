plugins {
    id("kotlin-build-tools-library")
}

description = "Kotlin Build Tools description of the Kotlin/Native build host"

dependencies {
    testImplementation(libs.junit.jupiter.params)
    // Test only: KotlinNativeHostTest compares the library with HostManager.
    testImplementation(project(":native:kotlin-native-utils"))
    // @MustRunOnChangesInNative for the tests that compare with HostManager
    testImplementation(project(":repo:test-runtime"))
}
