plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(project(":analysis:analysis-api"))
    testImplementation(project(":analysis:low-level-api-fir"))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(libs.lincheck)

    testRuntimeOnly(libs.intellij.fastutil)
}

sourceSets {
    "test" { projectDefault() }
}

configureJvmToolchain(JdkMajorVersion.JDK_11_0)

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir

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
}

testsJar()
