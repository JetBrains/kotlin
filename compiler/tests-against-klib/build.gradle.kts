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
    testRuntimeOnly(toolsJarApi())
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
    testData(project(":compiler").isolated, "testData/ir/irText")
    testData(project(":compiler").isolated, "testData/mockJDK")

    withJvmStdlibAndReflect()

    testTask(jUnitMode = JUnitMode.JUnit5)

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateCompilerTestsAgainstKlibKt", generateTestsInBuildDirectory = true)
}

optInToK1Deprecation()
val stdlibJvmIr by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val kotlinReflect by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val kotlinStdlibDependency by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val syncStdlib = tasks.register<Sync>("copyStdlib") {
    from(stdlibJvmIr)
    into(layout.buildDirectory.dir("stdlib-for-test"))
    rename { name ->
        when {
            name.endsWith(".klib") -> "stdlib.klib"
            name.endsWith(".jar") -> "stdlib.jar"
            else -> name
        }
    }
}

dependencies {
    stdlibJvmIr(project(":kotlin-stdlib-jklib-for-test", configuration = "distMinimalJKlib"))
    kotlinReflect(project(":kotlin-reflect")) { isTransitive = false }
    add(kotlinStdlibDependency.name, kotlinStdlib()) { isTransitive = false }
    testRuntimeOnly(files(syncStdlib))
}

tasks.named("generateTestsWriteClassPath") {
    dependsOn(syncStdlib)
}

tasks.withType<Test>().configureEach {
    inputs.files(syncStdlib)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("stdlibArtifacts")
    inputs.files(kotlinReflect)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("kotlinReflect")
    inputs.files(kotlinStdlibDependency)
        .withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("kotlinStdlib")

    val stdlibDir = layout.buildDirectory.dir("stdlib-for-test")
    val kotlinReflectFiles: FileCollection = kotlinReflect
    val kotlinStdlibFiles: FileCollection = kotlinStdlibDependency
    doFirst {
        systemProperty("kotlin.stdlib.jvm.ir.klib", stdlibDir.get().file("stdlib.klib").asFile.absolutePath)
        systemProperty("kotlin.stdlib.jvm.ir.jar", stdlibDir.get().file("stdlib.jar").asFile.absolutePath)
        systemProperty("kotlin.reflect.jar", kotlinReflectFiles.singleFile.absolutePath)
        systemProperty("kotlin.stdlib.jar", kotlinStdlibFiles.singleFile.absolutePath)
    }
}
