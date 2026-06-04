plugins {
    kotlin("jvm")
    id("require-explicit-types")
}

dependencies {
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:frontend.java"))
    api(project(":compiler:fir:fir-jvm"))
    api(project(":compiler:fir:fir-native"))
    api(project(":compiler:fir:raw-fir:psi2fir"))
    api(project(":compiler:fir:raw-fir:light-tree2fir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:checkers:checkers.native"))
    api(project(":compiler:fir:checkers:checkers.wasm"))
    api(project(":compiler:fir:fir-deserialization"))
    implementation(project(":wasm:wasm.frontend"))
    implementation(project(":native:frontend.native"))
    api(project(":compiler:ir.actualization"))

    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:fir-js"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:ir.serialization.jvm"))
    implementation(project(":compiler:ir.serialization.js"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    implementation(project(":js:js.config"))
    implementation(project(":js:js.frontend"))
    implementation(project(":js:js.frontend.common"))
    implementation(project(":kotlin-util-klib-metadata"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToK1Deprecation()
