plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { java.srcDirs("main") }
    "test" { projectDefault() }
}

dependencies {
    testApi(projectTests(":native:swift:sir-analysis-api"))
    testApi(projectTests(":native:swift:sir-compiler-bridge"))
    testImplementation(projectTests(":generators:test-generator"))

    testRuntimeOnly(projectTests(":analysis:analysis-test-framework"))
    testImplementation(libs.junit.jupiter.api)
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.native.swift.sir.GenerateSirTestsKt")

testsJar()

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}


projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform { }
}
