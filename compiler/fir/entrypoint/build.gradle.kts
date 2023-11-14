plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors.jvm"))
    api(project(":compiler:frontend.java"))
    api(project(":compiler:fir:java"))
    api(project(":compiler:fir:native"))
    api(project(":compiler:fir:raw-fir:psi2fir"))
    api(project(":compiler:fir:raw-fir:light-tree2fir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:checkers:checkers.native"))
    api(project(":compiler:fir:checkers:checkers.wasm"))
    api(project(":js:js.frontend"))
    api(project(":compiler:ir.actualization"))

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
