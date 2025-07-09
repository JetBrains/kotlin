plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":compiler:ir.signatures"))
    api(project(":compiler:serialization"))
    api(project(":kotlin-util-klib"))
    compileOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
