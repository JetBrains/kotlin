
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:ir.tree"))
}

optInToIrSymbolInternals()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
