plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":compiler:ir.tree.impl"))
    compileOnly(project(":kotlin-reflect-api"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

