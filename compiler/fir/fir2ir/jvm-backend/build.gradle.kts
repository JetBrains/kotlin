plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:descriptors"))
    compileOnly(project(":compiler:backend.common.jvm"))
    compileOnly(project(":compiler:fir:cones"))
    compileOnly(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:fir:providers"))
    compileOnly(project(":compiler:fir:semantics"))
    compileOnly(project(":compiler:fir:java"))
    compileOnly(project(":compiler:fir:tree"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compileOnly(project(":compiler:fir:fir-serialization"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:backend.jvm"))
    api(project(":compiler:ir.serialization.common"))
    compileOnly(project(":compiler:ir.actualization"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
}
