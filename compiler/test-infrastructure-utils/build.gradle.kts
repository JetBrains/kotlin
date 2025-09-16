plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("gradle-plugin-compiler-dependency-configuration")
    id("project-tests-convention")
}

dependencies {
    testFixturesApi(libs.opentest4j)
    testFixturesImplementation(project(":compiler:fir:entrypoint"))
    testFixturesImplementation(project(":compiler:cli"))
    testFixturesImplementation(intellijCore())
    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(kotlin("test"))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

testsJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}
