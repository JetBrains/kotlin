plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

dependencies {
    testApi(projectTests(":compiler"))

    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    testApi(commonDependency("com.google.code.gson:gson"))
    testApi(intellijJDom())

    api(libs.jsoup)

    testRuntimeOnly(project(":core:descriptors.runtime"))
    testRuntimeOnly(toolsJar())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.params)
    runtimeOnly(libs.junit.vintage.engine)
    testImplementation(libs.junit4)
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar()

compilerTests {
    testData("testData")
    withScriptRuntime()
    withTestJar()
}

projectTest(parallel = true) {
    workingDir = rootDir
    useJUnitPlatform()
    inputs.file(File(rootDir, "tests/mute-common.csv")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(File(rootDir, "compiler/cli/cli-common/resources/META-INF/extensions/compiler.xml"))
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(File(rootDir, "compiler/testData/mockJDK/jre/lib/rt.jar")).withNormalizer(ClasspathNormalizer::class)
}

val generateSpecTests by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateSpecTestsKt")

val generateFeatureInteractionSpecTestData by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateFeatureInteractionSpecTestDataKt")

val printSpecTestsStatistic by generator("org.jetbrains.kotlin.spec.utils.tasks.PrintSpecTestsStatisticKt")

val specConsistencyTests by task<Test> {
    workingDir = rootDir
    filter {
        includeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
    }
    useJUnitPlatform()
    inputs.dir(layout.projectDirectory.dir("testData")).withPathSensitivity(PathSensitivity.RELATIVE)
}

tasks.named<Test>("test") {
    filter {
        excludeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
    }
}
