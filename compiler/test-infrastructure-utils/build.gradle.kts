plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    testFixturesImplementation(libs.opentest4j)
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils.common")))
    testFixturesImplementation(project(":compiler:fir:entrypoint"))
    testFixturesImplementation(project(":compiler:cli"))
    testFixturesImplementation(project(":compiler:cli-jvm"))
    testFixturesImplementation(project(":compiler:backend.common.jvm"))
    testFixturesImplementation(project(":compiler:frontend"))
    testFixturesImplementation(project(":compiler:frontend.java"))
    testFixturesImplementation(project(":js:js.config"))
    testFixturesImplementation(libs.intellij.asm)
    testFixturesImplementation(intellijCore())
    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(kotlin("test"))
    // This line is needed for optInToK1Deprecation only
    testImplementation(project(":core:util.runtime"))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

optInToK1Deprecation()

testsJar()

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5)
}
