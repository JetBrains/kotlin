plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.backend.common"))
    api(project(":compiler:backend.jvm"))
    compileOnly(intellijCore())
}

optInToIrSymbolInternals()

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
