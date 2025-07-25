import org.jetbrains.kotlin.kotlinNativeDist

plugins {
    kotlin("jvm")
    id("compiler-tests-convention")
}

dependencies {
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(testFixtures(project(":analysis:analysis-api-standalone")))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(testFixtures(project(":native:native.tests")))
    testImplementation(project(":native:kotlin-native-utils"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { none() }
    "test" {
        projectDefault()
    }
}


compilerTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        dependsOn(":dist")
        workingDir = rootDir
    }
}

val test by nativeTest("test", null) {
    systemProperty("kotlin.native.home", kotlinNativeDist.absolutePath)
}

testsJar()
