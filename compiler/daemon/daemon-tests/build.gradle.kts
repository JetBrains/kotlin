description = "Kotlin Daemon Tests"

plugins {
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
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

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        val testClassesDirs = testSourceSet.output.classesDirs
        doFirst {
            systemProperty("kotlin.test.script.classpath", testClassesDirs.joinToString(File.pathSeparator))
        }

        systemProperty(
            "kotlin.daemon.custom.run.files.path.for.tests",
            "build/daemon"
        )

        extensions.configure<TestInputsCheckExtension> {
            with(extraPermissions) {
                add("permission java.net.SocketPermission \"localhost\", \"listen,connect,resolve,accept\";",)
                add("permission java.util.PropertyPermission \"java.rmi.server.hostname\", \"write\";")
                add("permission java.util.PropertyPermission \"kotlin.daemon.environment.variables.for.tests\", \"write\";")
                add("permission java.util.PropertyPermission \"kotlin.daemon.options\", \"write\";")
                add("permission java.util.PropertyPermission \"kotlin.daemon.jvm.options\", \"write\";")
            }
        }
    }

    @OptIn(KotlinCompilerDistUsage::class)
    withDist()

    testData(project(":compiler:tests-integration").isolated, "testData/integration/smoke/")
}
