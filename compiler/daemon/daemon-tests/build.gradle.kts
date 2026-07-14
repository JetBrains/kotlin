description = "Kotlin Daemon Tests"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(project(":kotlin-daemon"))
    testImplementation(project(":kotlin-daemon-client"))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(testFixtures(project(":compiler:tests-integration")))
    testImplementation(intellijCore())
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTests {
    testTask {
        addClasspathProperty(testSourceSet.output.classesDirs, "kotlin.test.script.classpath")
        systemProperty("kotlin.daemon.custom.run.files.path.for.tests", "build/daemon")
    }

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project(":compiler:tests-integration").isolated, "testData/integration/smoke/")
}
