plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(projectTests(":compiler"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    testCompile(intellijDep()) {
        includeJars("groovy", "groovy-xml", rootProject = rootProject)
    }
    testCompile(intellijDep()) {
        includeJars("gson", rootProject = rootProject)
    }
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijPluginDep("java"))
    compile("org.jsoup:jsoup:1.10.3")
    if (isIdeaActive) testRuntimeOnly(files("${rootProject.projectDir}/dist/kotlinc/lib/kotlin-reflect.jar"))
    testRuntime(project(":kotlin-reflect"))

    testApiJUnit5(vintageEngine = true)
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":dist")
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
}

tasks.named<Test>("test") {
    filter {
        excludeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
    }
}
