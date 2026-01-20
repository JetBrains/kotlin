plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

sourceSets {
    "main" { java.srcDirs("main") }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

dependencies {
    api(kotlinStdlib("jdk8"))

    testFixturesImplementation(testFixtures(project(":generators:test-generator")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-spec")))
    testFixturesImplementation(testFixtures(project(":analysis:low-level-api-fir")))
    testFixturesImplementation(testFixtures(project(":analysis:analysis-api-fir")))
    testFixturesImplementation(testFixtures(project(":analysis:analysis-api-fe10")))
    testFixturesImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testFixturesImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesImplementation(testFixtures(project(":analysis:decompiled:decompiler-to-file-stubs")))
    testFixturesImplementation(testFixtures(project(":analysis:decompiled:decompiler-to-psi")))
    testFixturesImplementation(testFixtures(project(":analysis:stubs")))
    testFixturesImplementation(testFixtures(project(":analysis:symbol-light-classes")))
    testFixturesImplementation(testFixtures(project(":analysis:decompiled:decompiler-native")))
    testFixturesImplementation(intellijCore())
}

val generateTests by generator("org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt", testSourceSet)

testsJar()
