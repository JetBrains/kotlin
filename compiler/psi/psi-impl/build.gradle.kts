plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend.common"))
    api(project(":kotlin-script-runtime"))
    implementation(project(":compiler:frontend.common.jvm"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)

    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:parser"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}