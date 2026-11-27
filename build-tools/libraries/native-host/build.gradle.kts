plugins {
    id("kotlin-build-tools-library")
}

description = "Kotlin Build Tools description of the Kotlin/Native build host"

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))

    testImplementation(libs.junit.jupiter.params)
    // Test only: the tests compare the library with HostManager.
    testImplementation(project(":native:kotlin-native-utils"))
    // @MustRunOnChangesInNative for the tests that compare with HostManager
    testImplementation(project(":repo:test-runtime"))
}
