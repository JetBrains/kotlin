plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    api(kotlinStdlib())
    implementation(project(":kotlin-util-klib"))
    implementation(project(":core:compiler.common"))
    implementation(project(":compiler:ir.serialization.common"))
    compileOnly(libs.intellij.fastutil)
    testImplementation(platform(libs.junit.bom))
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
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testData(project(":compiler").isolated, "testData/klib/dump-abi/content")
    testData(project(":compiler").isolated, "testData/klib/dump-abi/malformed")
    withStdlibJsRuntime()
    withTestJsRuntime()

    testTask(jUnitMode = JUnitMode.JUnit5) {
        outputs.dir(layout.buildDirectory.dir("t"))
        useJUnitPlatform()
    }

    testGenerator("org.jetbrains.kotlin.library.abi.GenerateLibraryAbiReaderTestsKt", generateTestsInBuildDirectory = true)
}

testsJar()
