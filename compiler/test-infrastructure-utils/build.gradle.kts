plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(project(":compiler:fir:entrypoint"))
    testFixturesImplementation(project(":compiler:cli"))
    testFixturesImplementation(intellijCore())
    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

testsJar()
