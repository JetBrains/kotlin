plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:backend.jvm"))
    api(project(":compiler:ir.tree.impl"))
    api(project(":compiler:ir.serialization.jvm"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
