plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

dependencies {
    implementation(project(":compiler:util"))
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:cli-metadata"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.psi2ir"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    implementation(project(":compiler:plugin-api"))
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:fir:fir-jvm"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":kotlin-util-io"))
    implementation(project(":kotlin-build-common"))
    implementation(project(":compiler:javac-wrapper"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:java-direct"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(commonDependency("org.jetbrains.intellij.deps:jdom:2.0.6"))
    compileOnly(libs.kotlinx.coroutines.core.jvm)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask()
}

optInToExperimentalCompilerApi()
