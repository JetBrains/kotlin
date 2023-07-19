plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:util"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:cli"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:backend-common"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.js"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:backend.wasm"))
    implementation(project(":wasm:wasm.frontend"))
    implementation(project(":wasm:wasm.config"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}
