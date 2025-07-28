plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":core:util.runtime"))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(kotlinStdlib())
    testFixturesImplementation(libs.junit4)
    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesApi(project(":generators"))

    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

testsJar {}
