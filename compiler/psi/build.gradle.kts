plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend.common"))
    api(project(":kotlin-script-runtime"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)

    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:psi:psi-impl"))
    api(project(":compiler:psi:psi-utils"))
    api(project(":compiler:psi:psi-frontend-utils"))
    api(project(":compiler:psi:parser"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}