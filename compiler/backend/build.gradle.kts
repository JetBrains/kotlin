plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":kotlin-annotations-jvm"))
    compile(project(":compiler:util"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:serialization"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "asm-all", "guava", rootProject = rootProject) }
    compileOnly(intellijDep()) { includeJars("trove4j", rootProject = rootProject) }
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
