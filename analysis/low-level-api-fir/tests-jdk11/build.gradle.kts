plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(project(":analysis:analysis-api"))
    testImplementation(project(":analysis:low-level-api-fir"))
    testImplementation("org.jetbrains.kotlinx:lincheck:2.23")
}

sourceSets {
    "test" { projectDefault() }
}

configureJvmToolchain(JdkMajorVersion.JDK_11_0)

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()

    // This is required by lincheck model checking to be able to use `jdk.internal.misc.Unsafe` and similar classes under the hood.
    jvmArgs(
        "--add-opens",
        "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports",
        "java.base/jdk.internal.util=ALL-UNNAMED",
        "--add-exports",
        "java.base/sun.security.action=ALL-UNNAMED"
    )
}

testsJar()
