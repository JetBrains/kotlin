plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("compiler-tests-convention")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        workingDir = rootDir
    }
}

dependencies {
    api(project(":core:deserialization"))
    api(project(":compiler:psi:psi-api"))
    api(project(":analysis:decompiled:decompiler-to-file-stubs"))
    api(project(":analysis:decompiled:decompiler-to-psi"))

    implementation(project(":js:js.serializer"))

    compileOnly(intellijCore())

    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common-new")))
}

testsJar()
