description = "Kotlin Build Common"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
}

dependencies {
    implementation(project(":compiler:arguments.common"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:resolution"))
    implementation(project(":compiler:serialization"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":compiler:backend.common.jvm"))
    compileOnly(project(":compiler:util"))
    compileOnly(project(":compiler:cli-base"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":js:js.config"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
    compileOnly(project(":compiler:build-tools:kotlin-build-statistics"))

    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(protobufFull())
    testFixturesCompileOnly(project(":compiler:cli-base"))
    testFixturesCompileOnly(project(":js:js.parser"))
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(libs.junit.jupiter.params)
    testFixturesImplementation(libs.junit4)
    testFixturesImplementation(project(":compiler:build-tools:kotlin-build-statistics"))
    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesImplementation("org.reflections:reflections:0.10.2")

    testCompileOnly(project(":compiler:cli-base"))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.junit4)
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(project(":compiler:build-tools:kotlin-build-statistics"))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation("org.reflections:reflections:0.10.2")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

optInToK1Deprecation()

// test jar is used for ide dependencies (`kotlin-build-common-tests-for-ide` and `kotlin-jps-plugin-tests-for-ide`)
testsJarToBeUsedAlongWithFixtures()

projectTests {
    testTask(parallel = true, jUnitMode = JUnitMode.JUnit4)
    testTask("testJUnit5", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false)
}
