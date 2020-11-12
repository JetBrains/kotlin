plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.serialization.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
