description = "Kotlin Build Common"

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
    id("java-test-fixtures")
    id("compiler-tests-convention")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:backend.common.jvm"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.serializer"))
    compileOnly(project(":js:js.config"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
    compileOnly(project(":compiler:build-tools:kotlin-build-statistics"))

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(protobufFull())
    testFixturesCompileOnly(project(":compiler:cli-common"))
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.junit.jupiter.params)
    testFixturesImplementation(libs.junit4)
    testFixturesImplementation(project(":compiler:build-tools:kotlin-build-statistics"))
    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesImplementation("org.reflections:reflections:0.10.2")

    testCompileOnly(project(":compiler:cli-common"))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit4)
    testImplementation(project(":compiler:build-tools:kotlin-build-statistics"))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation("org.reflections:reflections:0.10.2")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

testsJar()

compilerTests {
    testTask(parallel = true, jUnitMode = JUnitMode.JUnit4)
    testTask("testJUnit5", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false)
}
