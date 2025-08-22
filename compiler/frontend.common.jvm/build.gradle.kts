plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:metadata.jvm"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    api(project(":compiler:psi:psi-api"))
    api(project(":core:compiler.common.jvm"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.kotlinx.coroutines.core.jvm)
    compileOnly(libs.intellij.asm)
    compileOnly(libs.intellij.fastutil)
    implementation(libs.vavr)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
