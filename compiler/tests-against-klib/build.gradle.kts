plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
    id("java-test-fixtures")
}

dependencies {
    api(kotlinStdlib())
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-integration")))
    testFixturesImplementation(project(":compiler:cli-jklib"))

    testFixturesCompileOnly(intellijCore())
    testRuntimeOnly(intellijCore())
    testFixturesApi("org.junit.jupiter:junit-jupiter")
    testCompileOnly(libs.junit4)
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
    "main" { }
    "testFixtures" { projectDefault() }
}

projectTests {
    // only 2 files are really needed:
    // - compiler/testData/codegen/boxKlib/properties.kt
    // - compiler/testData/codegen/boxKlib/simple.kt
    testData(project(":compiler").isolated, "testData/codegen/boxKlib")
    testData(project(":compiler").isolated, "testData/codegen/jklib")

    withJvmStdlibAndReflect()

    testTask(jUnitMode = JUnitMode.JUnit5)

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestsAgainstKlibKt", generateTestsInBuildDirectory = true)
}

optInToK1Deprecation()
val stdlibJvmIr by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val stdlibJs by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val syncStdlib = tasks.register<Sync>("copyStdlib") {
    from(stdlibJvmIr)
    from(stdlibJs)
    into(layout.buildDirectory.dir("stdlib-for-test"))
    rename { name ->
        when {
            name.contains("js") && name.endsWith(".klib") -> "stdlib-js.klib"
            name.endsWith(".klib") -> "stdlib.klib"
            name.endsWith(".jar") -> "stdlib.jar"
            else -> name
        }
    }
}

dependencies {
    stdlibJvmIr(project(":kotlin-stdlib-jvm-ir-for-test", configuration = "distJKlib"))
    stdlibJs(project(":kotlin-stdlib", configuration = "distJsKlib"))
    testRuntimeOnly(files(syncStdlib))
}

tasks.named("generateTestsWriteClassPath") {
    dependsOn(syncStdlib)
}

tasks.withType<Test>().configureEach {
    // Register the task's output as an input. 
    // This tells Gradle (and the security plugin) that we read these files.
    inputs.files(syncStdlib)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("stdlibArtifacts")

    val stdlibDir = layout.buildDirectory.dir("stdlib-for-test")
    doFirst {
        systemProperty("kotlin.stdlib.jvm.ir.klib", stdlibDir.get().file("stdlib-js.klib").asFile.absolutePath)
        // systemProperty("kotlin.stdlib.jvm.ir.klib", stdlibDir.get().file("stdlib.klib").asFile.absolutePath)
        systemProperty("kotlin.stdlib.jvm.ir.jar", stdlibDir.get().file("stdlib.jar").asFile.absolutePath)
    }
}
