plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend-common"))
    compile(project(":compiler:ir.tree"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { // primary used in ir interpreter
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

