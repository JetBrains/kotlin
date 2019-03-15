
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:frontend.common"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:fir:resolve"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}