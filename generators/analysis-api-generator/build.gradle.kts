plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { java.srcDirs("main") }
    "test" { projectDefault() }
}

dependencies {
    api(kotlinStdlib("jdk8"))

    testImplementation(testFixtures(project(":generators:test-generator")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(projectTests(":compiler:tests-spec"))
    testImplementation(projectTests(":analysis:low-level-api-fir"))
    testImplementation(testFixtures(project(":analysis:analysis-api-fir")))
    testImplementation(testFixtures(project(":analysis:analysis-api-fe10")))
    testImplementation(projectTests(":analysis:analysis-api-standalone"))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(projectTests(":analysis:decompiled:decompiler-to-psi"))
    testImplementation(projectTests(":analysis:stubs"))
    testImplementation(projectTests(":analysis:symbol-light-classes"))
    testImplementation(projectTests(":analysis:decompiled:decompiler-native"))
    testImplementation(intellijCore())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

val generateFrontendApiTests by generator("org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt") {
    dependsOn(":generators:analysis-api-generator:generator-kotlin-native:generateAnalysisApiNativeTests")
}

testsJar()
