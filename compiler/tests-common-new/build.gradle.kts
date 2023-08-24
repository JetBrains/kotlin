plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:fir:fir-serialization"))
    testApi(project(":compiler:fir:fir2ir:jvm-backend"))
    testApi(project(":compiler:cli"))
    testImplementation(project(":compiler:ir.tree"))
    testImplementation(project(":compiler:backend.jvm.entrypoint"))
    testImplementation(project(":compiler:backend.jvm.lower"))
    testImplementation(intellijCore())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testRuntimeOnly(project(":core:descriptors.runtime"))

    testImplementation(projectTests(":generators:test-generator"))

    testApiJUnit5()
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:tests-compiler-utils"))

    /*
     * Actually those dependencies are needed only at runtime, but they
     *   declared as Api dependencies to propagate them to all modules
     *   which depend on current one
     */
    testApi(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    testApi(commonDependency("one.util:streamex"))
    testApi(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
    testApi(jpsModel()) { isTransitive = false }
    testApi(jpsModelImpl()) { isTransitive = false }
    testApi(intellijJavaRt())

    testApi(toolsJar())
}

optInToExperimentalCompilerApi()
optInToIrSymbolInternals()

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

fun Test.configureTest(configureJUnit: JUnitPlatformOptions.() -> Unit = {}) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform {
        configureJUnit()
    }
}

projectTest(
    jUnitMode = JUnitMode.JUnit5,
    defineJDKEnvVariables = listOf(
        JdkMajorVersion.JDK_11_0 // e.g. org.jetbrains.kotlin.test.runners.ForeignAnnotationsCompiledJavaTestGenerated.Java11Tests
    )
) {
    configureTest {
        excludeTags("Jdk21Test")
    }
}

// Separate configuration is only necessary while JDK 21 is not released, so cannot be obtained via toolchain.
// See KT-58765 for tracking
projectTest(
    "jdk21Tests",
    jUnitMode = JUnitMode.JUnit5,
    defineJDKEnvVariables = listOf(
        JdkMajorVersion.JDK_21_0
    )
) {
    configureTest {
        includeTags("Jdk21Test")
    }
}

testsJar()
