plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:fir:native"))
    implementation(project(":compiler:fir:raw-fir:psi2fir"))
    implementation(project(":compiler:fir:raw-fir:light-tree2fir"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:fir-deserialization"))
    implementation(project(":compiler:fir:checkers:checkers.jvm"))
    implementation(project(":compiler:fir:checkers:checkers.js"))
    implementation(project(":compiler:fir:checkers:checkers.native"))
    implementation(project(":compiler:fir:checkers:checkers.wasm"))
    implementation(project(":compiler:ir.actualization"))

    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.common"))
    implementation(project(":native:frontend.native"))
    implementation(project(":wasm:wasm.frontend"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
