
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compileOnly(intellijDep())
    compileOnly(intellijPluginDep("java"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

