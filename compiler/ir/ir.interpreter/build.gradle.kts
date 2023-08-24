plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
}

optInToIrSymbolInternals()

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

