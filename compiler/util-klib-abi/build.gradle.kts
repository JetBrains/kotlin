plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    implementation(project(":kotlin-util-klib"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:ir.serialization.common"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(intellijCore())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":generators:test-generator"))
    testImplementation(projectTests(":js:js.tests"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

val testDataDir = project(":compiler").projectDir.resolve("testData/klib/dump-abi/content")

projectTest(jUnitMode = JUnitMode.JUnit5) {
    inputs.dir(testDataDir)
    outputs.dir("$buildDir/t")

    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

val generateTests by generator("org.jetbrains.kotlin.library.abi.GenerateLibraryAbiReaderTestsKt")

testsJar()
