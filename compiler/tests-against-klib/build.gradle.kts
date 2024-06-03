plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

dependencies {
    api(kotlinStdlib())
    testApi(projectTests(":generators:test-generator"))
    testApi(projectTests(":compiler:tests-common"))

    testCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
    testImplementation("org.junit.jupiter:junit-jupiter")
    testCompileOnly(libs.junit4)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar {}

compilerTests {
    // only 2 files are really needed:
    // - compiler/testData/codegen/boxKlib/properties.kt
    // - compiler/testData/codegen/boxKlib/simple.kt
    testData("../testData/codegen/boxKlib")
}

projectTest(parallel = true) {
    workingDir = rootDir
    useJUnitPlatform()

    // only 2 files are really needed:
    // - compiler/testData/codegen/boxKlib/properties.kt
    // - compiler/testData/codegen/boxKlib/simple.kt
    inputs.dir(layout.projectDirectory.dir("../testData")).withPathSensitivity(PathSensitivity.RELATIVE)
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestsAgainstKlibKt")
