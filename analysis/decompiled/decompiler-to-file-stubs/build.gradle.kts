plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("project-tests-convention")
}

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    api(project(":core:deserialization"))
    implementation(project(":analysis:decompiled:decompiler-to-stubs"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":core:compiler.common.jvm"))

    api(intellijCore())

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.vintage.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir
    }

    withJvmStdlibAndReflect()
}


testsJar()
