plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("java-test-fixtures")
    id("test-inputs-check")
}

dependencies {
    implementation(kotlinStdlib())
    // The model of the reports is shared with the wrapper, which reads them
    api(libs.kotlinx.serialization.json)

    testFixturesImplementation(testFixtures(project(":compiler:test-infrastructure-utils.common")))
    testFixturesImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesImplementation(kotlinStdlib())
    testFixturesImplementation(libs.kotlinx.serialization.json)
    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.junit.platform.launcher)
    testFixturesImplementation(libs.junit.jupiter.engine)
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils.common")))
    testImplementation(testFixtures(project(":compiler:test-infrastructure-utils")))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask {
        filter {
            excludeTestsMatching("org.jetbrains.kotlin.analysis.test.data.manager.fakes.*")
            excludeTestsMatching("org.example.testdatamanager.fakes.*")
        }
    }

    withJvmStdlibAndReflect()
}
