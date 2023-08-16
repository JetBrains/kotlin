description = "Kotlin Daemon Tests"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(project(":kotlin-test:kotlin-test-jvm"))
    testImplementation(project(":kotlin-daemon"))
    testImplementation(project(":kotlin-daemon-client"))
    testImplementation(libs.junit4)
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(intellijCore())
}

sourceSets {
    "main" {}
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir

    val testClassesDirs = testSourceSet.output.classesDirs
    doFirst {
        systemProperty("kotlin.test.script.classpath", testClassesDirs.joinToString(File.pathSeparator))
    }
}
