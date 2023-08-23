plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:ir.tree"))

    implementation(project(":core:compiler.common.native"))

    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}

optInToIrSymbolInternals()
