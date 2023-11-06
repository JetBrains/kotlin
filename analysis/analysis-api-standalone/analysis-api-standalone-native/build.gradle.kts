import org.jetbrains.kotlin.kotlinNativeDist

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(projectTests(":analysis:analysis-api-standalone"))
    testImplementation(projectTests(":native:native.tests"))
    testImplementation(project(":native:kotlin-native-utils"))
    testImplementation(project(":kotlin-native:utilities:basic-utils"))

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


projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

val test by nativeTest("test", null) {
    systemProperty("kotlin.native.home", kotlinNativeDist.absolutePath)
}

testsJar()
