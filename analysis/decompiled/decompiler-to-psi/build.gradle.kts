plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common"))
    implementation(project(":analysis:light-classes-base"))
    implementation(project(":analysis:decompiled:decompiler-to-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(intellijCore())

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

testsJar()