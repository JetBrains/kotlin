plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":js:js.frontend"))
    implementation(project(":compiler:ir.backend.common"))
    compile(project(":compiler:ir.tree.persistent"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    implementation(kotlin("reflect"))
}

sourceSets {
    "main" { projectDefault() }
}
