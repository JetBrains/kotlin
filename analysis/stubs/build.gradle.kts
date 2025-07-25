plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(intellijCore())

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesApi(testFixtures(project(":analysis:decompiled:decompiler-to-file-stubs")))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
    }
}

testsJar()
