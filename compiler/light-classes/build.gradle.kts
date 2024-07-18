plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":analysis:light-classes-base"))
    api(project(":compiler:util"))
    api(project(":compiler:backend"))
    api(project(":compiler:frontend"))
    api(project(":compiler:frontend.java"))
    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { projectDefault() }
}
