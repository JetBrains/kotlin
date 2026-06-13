plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesImplementation(libs.junit.platform.launcher)
    testFixturesImplementation(libs.junit.jupiter.engine)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}
