
plugins {
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    testFixturesApi(kotlinStdlib("jdk8"))
    testFixturesApi(project(":kotlin-scripting-compiler"))
    testFixturesApi(project(":core:descriptors"))
    testFixturesApi(project(":core:descriptors.jvm"))
    testFixturesApi(project(":core:deserialization"))
    testFixturesApi(project(":compiler:util"))
    testFixturesApi(project(":compiler:tests-mutes"))
    testFixturesApi(project(":compiler:backend"))
    testFixturesApi(project(":compiler:frontend"))
    testFixturesApi(project(":compiler:frontend.java"))
    testFixturesApi(project(":compiler:util"))
    testFixturesApi(project(":compiler:psi:psi-api"))
    testFixturesApi(project(":compiler:cli-common"))
    testFixturesApi(project(":compiler:cli"))
    testFixturesApi(project(":compiler:cli-js"))
    testFixturesApi(project(":compiler:serialization"))
    testFixturesApi(project(":compiler:fir:entrypoint"))
    testFixturesApi(project(":compiler:fir:fir2ir:jvm-backend"))
    testFixturesApi(project(":compiler:backend.jvm.entrypoint"))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(project(":kotlin-preloader"))
    testFixturesApi(commonDependency("com.android.tools:r8"))
    testFixturesCompileOnly(intellijCore())

    testFixturesApi(libs.guava)
    testFixturesApi(libs.intellij.asm)
    testFixturesApi(commonDependency("org.jetbrains.intellij.deps:log4j"))
    testFixturesApi(intellijJDom())
}

optInToUnsafeDuringIrConstructionAPI()
optInToK1Deprecation()

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

testsJar {}
