plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:backend-common"))
    api(project(":compiler:ir.tree"))
    compileOnly(intellijCore())
}

optInToIrSymbolInternals()
optInToObsoleteDescriptorBasedAPI()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
