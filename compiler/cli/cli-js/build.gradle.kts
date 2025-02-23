plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("share-kotlin-wasm-custom-formatters")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:cli-common"))
    api(project(":compiler:cli"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common"))
    implementation(project(":compiler:fir:entrypoint"))
    api(project(":compiler:fir:fir-serialization"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.serialization.js"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:backend.js"))
    api(project(":compiler:backend.wasm"))
    api(project(":js:js.sourcemap"))
    api(project(":wasm:wasm.frontend"))
    api(project(":wasm:wasm.config"))

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