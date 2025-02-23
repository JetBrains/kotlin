plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("share-kotlin-wasm-custom-formatters")
}

dependencies {
    implementation(project(":compiler:util"))
    api(project(":compiler:cli-common"))
    api(project(":compiler:cli"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:backend-common"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:ir.actualization"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.js"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:backend.js"))
    implementation(project(":compiler:backend.wasm"))
    implementation(project(":js:js.sourcemap"))
    implementation(project(":wasm:wasm.frontend"))
    implementation(project(":wasm:wasm.config"))

    wasmCustomFormatters(project(":wasm:wasm.debug.browsers"))

    compileOnly(intellijCore())
}

val updateWasmResources by tasks.registering(Sync::class) {
    from(configurations.wasmCustomFormattersResolver)
    into(temporaryDir)
}

sourceSets {
    "main" {
        projectDefault()
        resources.srcDir(updateWasmResources)
    }
}