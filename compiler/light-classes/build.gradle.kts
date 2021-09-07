
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }

    compileOnly(intellijDep()) { includeJars("platform-core-ui", "platform-util-ui") }

    compileOnly("org.jetbrains.intellij.deps:asm-all:9.1")
    compileOnly(intellijDep()) { includeJars("trove4j", "guava", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

