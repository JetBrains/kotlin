plugins {
    id("kotlin-build-tools-library")
}

description = "Kotlin Build Tools wrappers for Apple developer tools: Xcode discovery, xcodebuild, lipo, swift and others"

tasks.named<Test>("test") {
    // AppleToolsMacosTest runs the tools of the selected Xcode. Without these inputs, the build cache would reuse a
    // result from a Linux host, where those tests are skipped, or from a machine with another Xcode.
    inputs.files("/var/db/xcode_select_link/usr/bin/xcodebuild")
        .withPropertyName("selectedXcodebuild")
        .withPathSensitivity(PathSensitivity.ABSOLUTE)
    inputs.property("developerDir", providers.environmentVariable("DEVELOPER_DIR").orElse(""))
}
