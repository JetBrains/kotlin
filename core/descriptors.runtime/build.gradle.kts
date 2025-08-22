plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("jps-compatible")
    id("project-tests-convention")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":core:descriptors.jvm"))

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":generators:test-generator")))
    testFixturesApi(intellijCore())

    testFixturesApi(platform(libs.junit.bom))
    testImplementation(libs.junit4)
}

sourceSets {
    "main" { projectDefault() }
    "testFixtures" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        dependsOn(":dist")
        workingDir = rootDir
    }

    testGenerator("org.jetbrains.kotlin.generators.tests.GenerateRuntimeDescriptorTestsKt")

    withJvmStdlibAndReflect()
}

testsJar()
