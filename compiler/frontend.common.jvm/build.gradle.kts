plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":core:deserialization.common.jvm"))
    api(project(":compiler:psi:psi-api"))
    api(project(":core:compiler.common.jvm"))
    compileOnly(intellijCore())
    compileOnly(libs.kotlinx.coroutines.core.jvm)
    compileOnly(libs.intellij.asm)
    compileOnly(libs.intellij.fastutil)
    implementation(libs.vavr)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
