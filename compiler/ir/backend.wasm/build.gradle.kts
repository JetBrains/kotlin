plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.inline"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.serialization.js"))
    api(project(":js:js.ast"))
    api(project(":js:js.frontend"))
    api(project(":compiler:backend.js"))
    api(project(":wasm:wasm.ir"))

    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
