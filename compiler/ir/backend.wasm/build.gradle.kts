plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:cli-base"))
    api(project(":compiler:util"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.inline"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.serialization.js"))
    api(project(":js:js.ast"))
    api(project(":compiler:backend.js"))
    api(project(":wasm:wasm.ir"))

    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":wasm:wasm.frontend"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":core:compiler.common.wasm"))
    implementation(project(":core:compiler.common.js"))
    implementation(project(":core:descriptors"))
    implementation(project(":compiler:ir.psi2ir"))

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
