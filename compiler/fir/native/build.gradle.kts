plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":core:compiler.common.native"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:ir.serialization.common"))
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
