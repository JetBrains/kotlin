plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(projectTests(":compiler"))

    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    testApi(commonDependency("com.google.code.gson:gson"))
    testApi(commonDependency("org.jetbrains.intellij.deps:jdom"))

    api("org.jsoup:jsoup:1.14.2")

    testRuntimeOnly(project(":core:descriptors.runtime"))

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
