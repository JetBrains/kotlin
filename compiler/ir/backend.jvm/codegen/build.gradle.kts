plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.jvm"))
    implementation(project(":compiler:ir.tree.impl"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
