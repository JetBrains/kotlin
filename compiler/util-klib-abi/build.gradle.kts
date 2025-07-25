plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

dependencies {
    api(kotlinStdlib())
    implementation(project(":kotlin-util-klib"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:ir.serialization.common"))
    compileOnly(libs.intellij.fastutil)
    testApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(libs.junit.jupiter.params)
    testFixturesApi(intellijCore())
    testFixturesApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testFixturesApi(testFixtures(project(":js:js.tests")))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

val testDataDir = project(":compiler").projectDir.resolve("testData/klib/dump-abi/content")

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        inputs.dir(testDataDir)
        outputs.dir(layout.buildDirectory.dir("t"))

        dependsOn(":dist")
        workingDir = rootDir
        useJUnitPlatform()
    }

    testGenerator("org.jetbrains.kotlin.library.abi.GenerateLibraryAbiReaderTestsKt")
}

testsJar()
