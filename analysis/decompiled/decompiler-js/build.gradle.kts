plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

dependencies {
    api(project(":core:deserialization"))
    api(project(":compiler:psi"))
    api(project(":analysis:decompiled:decompiler-to-file-stubs"))
    api(project(":analysis:decompiled:decompiler-to-psi"))
    api(project(":js:js.serializer"))
    compileOnly(intellijCore())

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
}

testsJar()
