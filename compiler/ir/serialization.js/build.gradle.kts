plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":js:js.frontend"))

    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":wasm:wasm.config"))

    compileOnly(intellijCore())
    compileOnly(project(":compiler:cli-common"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
}
