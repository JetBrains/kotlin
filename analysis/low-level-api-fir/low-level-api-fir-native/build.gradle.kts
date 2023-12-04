plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(project(":analysis:low-level-api-fir"))
    testImplementation(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":analysis:analysis-api-impl-barebone"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:low-level-api-fir"))
    testImplementation(projectTests(":native:native.tests"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}


projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

nativeTest("llFirNativeTests", "llFirNative")

testsJar()
