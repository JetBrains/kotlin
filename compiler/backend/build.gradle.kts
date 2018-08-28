
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-annotations-jvm"))
    compile(project(":compiler:util"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":compiler:serialization"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "annotations", "asm-all", rootProject = rootProject) }
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../ir/backend.jvm/src")
    }
    "test" {}
}

