plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:psi"))
    compile(project(":compiler:container"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}


sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}