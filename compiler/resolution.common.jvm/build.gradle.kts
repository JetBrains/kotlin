plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:resolution.common"))
    api(project(":core:compiler.common.jvm"))
    api(project(":compiler:psi"))
    implementation(project(":compiler:util"))
    implementation(libs.vavr)
    compileOnly(intellijCore())
    compileOnly(libs.kotlinx.coroutines.core.jvm)
    compileOnly(libs.intellij.asm)
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
