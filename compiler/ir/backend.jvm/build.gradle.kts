plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-annotations-jvm"))
    compile(project(":compiler:fir:fir-serialization"))
    compile(project(":compiler:fir:fir2ir"))
    compile(project(":compiler:fir:fir2ir:jvm-backend"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:ir.tree"))
    compile(project(":compiler:ir.psi2ir"))
    compile(project(":compiler:ir.backend.common"))
    compile(project(":compiler:ir.serialization.jvm"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", "guava", rootProject = rootProject) }
    compileOnly(intellijDep()) { includeJars("trove4j", rootProject = rootProject) }
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
