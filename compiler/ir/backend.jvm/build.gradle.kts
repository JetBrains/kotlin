plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-annotations-jvm"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":compiler:ir.backend.common"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "annotations", "asm-all", rootProject = rootProject) }
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
