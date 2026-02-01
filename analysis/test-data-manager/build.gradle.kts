plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
    id("project-tests-convention")
}

dependencies {
    testFixturesImplementation(testFixtures(project(":compiler:test-infrastructure-utils.common")))
    testFixturesImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesImplementation(kotlinStdlib())
    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.junit.platform.launcher)
    testFixturesImplementation(libs.junit.jupiter.engine)
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils.common")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(kotlinStdlib())
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        filter {
            excludeTestsMatching("org.jetbrains.kotlin.analysis.test.data.manager.fakes.*")
        }
    }

    withJvmStdlibAndReflect()
}
