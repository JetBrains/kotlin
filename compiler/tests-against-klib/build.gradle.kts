plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
    id("test-inputs-check")
}

dependencies {
    api(kotlinStdlib())
    testApi(projectTests(":generators:test-generator"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:tests-integration"))

    testCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
    testImplementation("org.junit.jupiter:junit-jupiter")
    testCompileOnly(libs.junit4)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    "main" { }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

compilerTests {
    // only 2 files are really needed:
    // - compiler/testData/codegen/boxKlib/properties.kt
    // - compiler/testData/codegen/boxKlib/simple.kt
    testData(project(":compiler").isolated, "testData/codegen/boxKlib")

    testData(project(":compiler").isolated, "testData/codegen/jklib")
}

projectTest(parallel = true) {
    useJUnitPlatform()
}

optInToK1Deprecation()

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestsAgainstKlibKt")
