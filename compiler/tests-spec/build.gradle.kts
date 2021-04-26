plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testCompile(projectTests(":compiler"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testRuntimeOnly(platform("org.junit:junit-bom:5.7.0"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter")

    testCompileOnly(intellijDep()) {
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
}

sourceSets {
    "main" { }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    workingDir = rootDir
}

val generateSpecTests by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateSpecTestsKt")

val generateFeatureInteractionSpecTestData by generator("org.jetbrains.kotlin.spec.utils.tasks.GenerateFeatureInteractionSpecTestDataKt")

val printSpecTestsStatistic by generator("org.jetbrains.kotlin.spec.utils.tasks.PrintSpecTestsStatisticKt")

val specConsistencyTests by task<Test> {
    workingDir = rootDir

    filter {
        includeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
    }
}

tasks.named<Test>("test") {
    filter {
        excludeTestsMatching("org.jetbrains.kotlin.spec.consistency.SpecTestsConsistencyTest")
    }
}
