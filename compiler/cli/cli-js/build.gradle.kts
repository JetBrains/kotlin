plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:cli-common"))
    api(project(":compiler:cli"))
    implementation(project(":compiler:cli-wasm"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:fir:fir-serialization"))
    api(project(":compiler:fir:fir2ir:jvm-backend")) // TODO needed for `FirJvmKotlinMangler`, but obviously wrong
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.serialization.js"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:backend.js"))
    api(project(":js:js.translator"))
    api(project(":js:js.serializer"))
    api(project(":js:js.dce"))
    api(project(":js:js.sourcemap"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}
