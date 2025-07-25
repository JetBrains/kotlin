description = "Kotlin Daemon Tests"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(kotlinTest("junit"))
    testImplementation(project(":kotlin-daemon"))
    testImplementation(project(":kotlin-daemon-client"))
    testImplementation(libs.junit4)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(testFixtures(project(":compiler:tests-integration")))
    testImplementation(intellijCore())
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.vintage.engine)
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir

        val testClassesDirs = testSourceSet.output.classesDirs
        doFirst {
            systemProperty("kotlin.test.script.classpath", testClassesDirs.joinToString(File.pathSeparator))
        }
    }
}
