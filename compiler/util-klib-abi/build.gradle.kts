plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    api(project(":kotlin-util-klib"))
    api(project(":core:compiler.common"))
    api(project(":compiler:ir.serialization.common"))
    testApiJUnit5()
    testImplementation(intellijCore())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":generators:test-generator"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

val testDataDir = projectDir.resolve("testData")

projectTest(jUnitMode = JUnitMode.JUnit5) {
    inputs.dir(testDataDir)
    outputs.dir("$buildDir/t")

    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

val generateTests by generator("org.jetbrains.kotlin.library.abi.GenerateLibraryAbiReaderTestsKt")

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader"
    }
}
