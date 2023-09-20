plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:ir.interpreter"))
    compileOnly(intellijCore())
}

optInToIrSymbolInternals()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

