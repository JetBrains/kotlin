plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":compiler:fir:entrypoint"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(intellijCore())

    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}
