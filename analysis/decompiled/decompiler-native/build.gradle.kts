plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
    id("project-tests-convention")
}

sourceSets {
    "main" { projectDefault() }
    "test" { generatedTestDir() }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        workingDir = rootDir
    }

    withJvmStdlibAndReflect()
}

dependencies {
    api(project(":core:deserialization"))
    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:frontend.java"))
    api(project(":analysis:decompiled:decompiler-to-file-stubs"))
    api(project(":analysis:decompiled:decompiler-to-psi"))
    api(project(":analysis:decompiled:decompiler-to-stubs"))
    api(project(":kotlin-util-klib-metadata"))

    implementation(project(":js:js.serializer"))

    compileOnly(intellijCore())

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit4)
    testCompileOnly(libs.junit.jupiter.api) // the annotations are misused and have no effect
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:decompiled:decompiler-to-file-stubs")))
}

optInToK1Deprecation()

testsJar()
