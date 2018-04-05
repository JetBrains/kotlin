
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:frontend.common"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:fir:resolve"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", "annotations") }
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}