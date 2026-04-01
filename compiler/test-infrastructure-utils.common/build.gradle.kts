plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    testFixturesImplementation(kotlinStdlib())
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
