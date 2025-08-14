plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

dependencies {
    testFixturesApi(project(":analysis:low-level-api-fir"))
    testFixturesApi(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { none() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir
    }

    nativeTestTask("llFirNativeTests", "llFirNative", requirePlatformLibs = true)
}

testsJar()
