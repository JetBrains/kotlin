/**
 * The command-line wrapper of the test data manager, see `README.md` of `:analysis:test-data-manager`.
 *
 * It runs before anything else of the repository is built, so it depends only on the standard library, the rules it
 * shares with the runner, kotlinx.serialization and the Gradle Tooling API. The `test-data-manager` launcher next to the parent module runs
 * it from the directory `installCli` fills.
 */
plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("test-inputs-check")
}

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":analysis:test-data-manager"))
    implementation(libs.gradle.toolingApi)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// `gradlew` needs Java 17 as well
jvmToolchains {
    jdkVersion = JdkMajorVersion.JDK_17_0
    targetBytecodeVersion = JdkMajorVersion.JDK_17_0
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask()
}

// The classpath of the CLI in one directory: its JAR and its runtime dependencies
tasks.register<Sync>("installCli") {
    description = "Collects the CLI of the test data manager and its dependencies for the launcher"
    from(tasks.jar)
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("cli"))
}
