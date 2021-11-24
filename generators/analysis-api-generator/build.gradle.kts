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

    testApi(projectTests(":generators:test-generator"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:tests-spec"))
    testApi(projectTests("::analysis:low-level-api-fir"))
    testApi(projectTests(":analysis:analysis-api-fir"))
    testApi(projectTests(":analysis:analysis-api-fe10"))
    testApi(intellijCore())
    testApiJUnit5()
}

val generateFrontendApiTests by generator("org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt")

testsJar()