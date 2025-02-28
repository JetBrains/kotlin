plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:psi"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(intellijCore())

    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:low-level-api-fir"))
    testImplementation(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir
    useJUnitPlatform()
}

testsJar()
