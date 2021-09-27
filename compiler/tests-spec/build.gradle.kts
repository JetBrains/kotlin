plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(projectTests(":compiler"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    testApi(intellijDep()) {
        includeJars("gson", "groovy", "groovy-xml", rootProject = rootProject)
    }
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntimeOnly(intellijDep()) {
        includeJars("streamex", rootProject = rootProject)
    }

    testRuntimeOnly(intellijPluginDep("java"))
    api("org.jsoup:jsoup:1.14.2")
    if (isIdeaActive) testRuntimeOnly(files("${rootProject.projectDir}/dist/kotlinc/lib/kotlin-reflect.jar"))
    testRuntimeOnly(project(":kotlin-reflect"))

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
