plugins {
    kotlin("jvm")
    id("java-instrumentation")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common.jvm"))
    api(project(":compiler:config.jvm"))
    api(libs.intellij.asm)
    api(libs.guava)
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
