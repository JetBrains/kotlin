plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:backend.jvm"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.serialization.jvm"))
    implementation(project(":compiler:backend.jvm.lower"))
    implementation(project(":compiler:backend.jvm.codegen"))
    compileOnly(intellijCore())
}

optInToIrSymbolInternals()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
