plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:ir.backend.common"))
    compile(project(":compiler:ir.tree"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

