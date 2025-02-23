
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testApi(kotlinStdlib("jdk8"))
    testImplementation(project(":analysis:light-classes-base"))
    testApi(project(":kotlin-scripting-compiler"))
    testApi(project(":core:descriptors"))
    testApi(project(":core:descriptors.jvm"))
    testApi(project(":core:deserialization"))
    testApi(project(":compiler:util"))
    testApi(project(":compiler:tests-mutes"))
    testApi(project(":compiler:backend"))
    testApi(project(":compiler:backend.js"))
    testImplementation(project(":compiler:fir:java"))
    testApi(project(":compiler:frontend"))
    testApi(project(":compiler:frontend.java"))
    testImplementation(project(":compiler:ir.actualization"))
    testApi(project(":compiler:util"))
    testApi(project(":compiler:psi"))
    testApi(project(":compiler:cli-common"))
    testApi(project(":compiler:cli"))
    testApi(project(":compiler:cli-js"))
    testApi(project(":compiler:serialization"))
    testApi(project(":compiler:fir:entrypoint"))
    testApi(project(":compiler:fir:fir2ir:jvm-backend"))
    testApi(project(":compiler:backend.jvm.entrypoint"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(project(":kotlin-preloader"))
    testImplementation(project(":native:frontend.native"))
    testApi(commonDependency("com.android.tools:r8"))
    testCompileOnly(intellijCore())

    testApi(libs.guava)
    testApi(libs.intellij.asm)
    testApi(commonDependency("org.jetbrains.intellij.deps:log4j"))
    testApi(intellijJDom())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

testsJar {}
