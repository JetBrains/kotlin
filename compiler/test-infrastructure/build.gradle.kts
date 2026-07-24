plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(project(":compiler:fir:entrypoint"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":compiler:cli-metadata"))
    testFixturesApi(project(":native:native.config"))
    testFixturesImplementation(project(":core:descriptors"))
    testFixturesImplementation(project(":core:language.targets.jvm"))
    testFixturesImplementation(project(":compiler:container"))
    testFixturesImplementation(project(":compiler:config.jvm"))
    testFixturesImplementation(project(":js:js.config"))
    testFixturesImplementation(project(":wasm:wasm.config"))
    testFixturesApi(intellijCore())

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(libs.junit.platform.launcher)
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))

    testFixturesImplementation(project(":js:js.config"))

    testRuntimeOnly(commonDependency("org.jetbrains.intellij.deps.jna:jna"))
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

optInToK1Deprecation()
