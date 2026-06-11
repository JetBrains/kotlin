plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":core:metadata.jvm"))
    api(project(":core:deserialization.common"))
    api(project(":core:deserialization.common.jvm"))
    api(project(":compiler:psi:psi-api"))
    api(project(":core:compiler.common.jvm"))
    implementation(project(":kotlin-script-runtime"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:config"))
    implementation(project(":compiler:frontend.common"))
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
