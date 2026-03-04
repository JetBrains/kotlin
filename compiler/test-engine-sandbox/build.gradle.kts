import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("jvm")
    id("d8-configuration")
    id("java-test-fixtures")
    id("project-tests-convention")
}

// WARNING: Native target is host-dependent. Re-running the same build on another host OS may give a different result.
val nativeTargetName = HostManager.host.name


val sandboxPluginForTests by configurations.creating

dependencies {
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:plugin-utils"))
    compileOnly(project(":compiler:fir:checkers"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:fir:entrypoint"))
    compileOnly(project(":compiler:plugin-api"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesCompileOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:fir:analysis-tests")))
    testFixturesApi(testFixtures(project(":native:native.tests:klib-ir-inliner")))

    testFixturesApi(testFixtures(project(":native:native.tests")))

    testRuntimeOnly(toolsJar())
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "testFixtures" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    nativeTestTask("test") {
        useJUnitPlatform {
            includeEngines("kotlin-compiler-grouping-engine", "junit-jupiter")
        }
        workingDir = rootDir
    }

    withJvmStdlibAndReflect()
    withScriptRuntime()
    withMockJdkAnnotationsJar()
    withMockJdkRuntime()
    withTestJar()
    withStdlibCommon()
    withJsRuntime()

    testData(project(":compiler").isolated, "testData/codegen/box")
}
