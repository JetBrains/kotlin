
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compileOnly(intellijDep())
    Platform[192].orHigher {
        compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

