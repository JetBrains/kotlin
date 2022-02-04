plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":js:js.frontend"))
    api(project(":compiler:ir.tree.persistent"))
    api(project(":compiler:fir:fir-serialization"))

    implementation(project(":compiler:ir.backend.common"))
    compileOnly(project(":kotlin-reflect-api"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
}
