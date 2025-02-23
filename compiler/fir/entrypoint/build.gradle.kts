plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:fir:java"))
    implementation(project(":compiler:fir:native"))
    api(project(":compiler:fir:raw-fir:psi2fir"))
    api(project(":compiler:fir:raw-fir:light-tree2fir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:checkers"))
    implementation(project(":compiler:fir:checkers:checkers.jvm"))
    implementation(project(":compiler:fir:checkers:checkers.js"))
    implementation(project(":compiler:fir:checkers:checkers.native"))
    implementation(project(":compiler:fir:checkers:checkers.wasm"))
    implementation(project(":wasm:wasm.frontend"))
    implementation(project(":native:frontend.native"))
    implementation(project(":compiler:ir.actualization"))

    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:fir:fir2ir:jvm-backend"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:ir.serialization.jvm"))
    implementation(project(":compiler:ir.serialization.js"))
    implementation(project(":compiler:ir.tree"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
