plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":compiler:ir.serialization.common"))
    compile(project(":compiler:ir.serialization.js"))
    compile(project(":compiler:ir.tree.persistent"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.frontend"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
