plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
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
    implementation(project(":compiler:serialization"))
    implementation(project(":compiler:plugin-api"))
    implementation(commonDependency("org.fusesource.jansi", "jansi"))
    implementation(project(":compiler:fir:raw-fir:psi2fir"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:providers"))
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:fir:fir-jvm"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":compiler:fir:checkers"))
    implementation(project(":compiler:fir:checkers:checkers.jvm"))
    implementation(project(":compiler:fir:checkers:checkers.js"))
    implementation(project(":compiler:fir:checkers:checkers.native"))
    implementation(project(":compiler:fir:checkers:checkers.wasm"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":kotlin-util-io"))
    implementation(project(":kotlin-build-common"))
    implementation(project(":compiler:javac-wrapper"))
    implementation(project(":analysis:light-classes-base"))
    implementation(project(":kotlin-util-klib-metadata"))

    compileOnly(toolsJarApi())
    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(commonDependency("org.jetbrains.intellij.deps:jdom:2.0.6"))
    compileOnly(libs.kotlinx.coroutines.core.jvm)
}

sourceSets {
    "main" { projectDefault() }
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()
