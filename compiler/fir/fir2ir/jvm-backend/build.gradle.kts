plugins {
    kotlin("jvm")
    id("require-explicit-types")
}

dependencies {
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:fir:cones"))
    implementation(project(":compiler:fir:resolve"))
    implementation(project(":compiler:fir:providers"))
    implementation(project(":compiler:fir:semantics"))
    implementation(project(":compiler:fir:fir-jvm"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:fir:fir2ir"))
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:ir.tree"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:backend.jvm"))
    api(project(":compiler:ir.serialization.common"))
    implementation(project(":compiler:ir.actualization"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:serialization"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
}

optInToK1Deprecation()
