
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(intellijDep())
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

