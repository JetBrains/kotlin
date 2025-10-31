plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":core:util.runtime"))
    testFixturesApi(project(":generators"))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils.common")))

    testFixturesApi(kotlinStdlib())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit4)
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(intellijCore())

    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

testsJar {}
