plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.backend.native"))
    compileOnly(project(":compiler:ir.serialization.native"))
    compileOnly(project(":compiler:ir.serialization.common"))
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

