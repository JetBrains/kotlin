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
    api(project(":compiler:frontend.java"))
    api(project(":analysis:decompiled:decompiler-to-file-stubs"))
    api(project(":analysis:decompiled:decompiler-to-psi"))
    api(project(":analysis:decompiled:decompiler-to-stubs"))
    api(project(":js:js.serializer"))
    api(project(":kotlin-util-klib-metadata"))
    compileOnly(intellijCore())
}

testsJar()
