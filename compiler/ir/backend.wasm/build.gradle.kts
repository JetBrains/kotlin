plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":compiler:ir.tree.impl"))
    compile(project(":compiler:backend.js"))
    compile(project(":wasm:wasm.ir"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
