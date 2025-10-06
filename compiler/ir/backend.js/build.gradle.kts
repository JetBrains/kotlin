plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:cli-base"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.inline"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.serialization.js"))
    api(project(":js:js.ast"))
    api(project(":js:js.sourcemap"))
    implementation(project(":js:js.translator"))

    // TODO(KT-79631): Remove these dependencies when we rewrite TS export to Analysis API
    api(project(":js:typescript-export-model"))
    api(project(":js:typescript-printer"))

    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
