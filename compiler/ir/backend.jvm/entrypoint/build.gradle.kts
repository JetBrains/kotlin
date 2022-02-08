plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:backend.jvm"))
    api(project(":compiler:ir.tree.impl"))
    api(project(":compiler:ir.serialization.jvm"))
    implementation(project(":compiler:backend.jvm.lower"))
    implementation(project(":compiler:backend.jvm.codegen"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
