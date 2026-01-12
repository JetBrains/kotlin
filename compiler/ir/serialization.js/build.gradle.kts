plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":js:js.frontend"))

    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":core:compiler.common.wasm"))

    compileOnly(intellijCore())
    compileOnly(project(":compiler:cli-common"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
}
