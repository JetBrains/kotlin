plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:cli-base"))
    api(project(":compiler:util"))
    implementation(project(":core:compiler.common.js"))
    implementation(project(":core:descriptors"))
    implementation(project(":compiler:frontend"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:ir.inline"))
    implementation(project(":compiler:ir.psi2ir"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:ir.serialization.js"))
    api(project(":js:js.ast"))
    implementation(project(":js:js.frontend"))
    implementation(project(":js:js.parser"))
    api(project(":js:js.sourcemap"))

    implementation(project(":core:compiler.common.js"))
    implementation(project(":js:js.config"))
    implementation(project(":js:js.translator"))
    implementation(project(":js:js.parser"))
    implementation(project(":kotlin-util-klib-metadata"))

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

optInToK1Deprecation()
