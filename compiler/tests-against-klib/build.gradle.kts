plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
    id("test-inputs-check")
    id("java-test-fixtures")
}

dependencies {
    api(kotlinStdlib())
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-integration")))

    testFixturesCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
    testFixturesApi("org.junit.jupiter:junit-jupiter")
    testCompileOnly(libs.junit4)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    "main" { }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

compilerTests {
    // only 2 files are really needed:
    // - compiler/testData/codegen/boxKlib/properties.kt
    // - compiler/testData/codegen/boxKlib/simple.kt
    testData(project(":compiler").isolated, "testData/codegen/boxKlib")

    testTask(jUnitMode = JUnitMode.JUnit5)

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestsAgainstKlibKt")
}

optInToK1Deprecation()
