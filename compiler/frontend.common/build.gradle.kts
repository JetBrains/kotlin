plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:psi"))
    compile(project(":compiler:container"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "annotations") }
}


sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}