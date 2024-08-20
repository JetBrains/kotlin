plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:descriptors"))
    api(project(":core:descriptors.jvm"))
    api(project(":compiler:util"))
    api(project(":compiler:config.jvm"))
    api("javax.annotation:jsr250-api:1.0")
    api(project(":compiler:frontend"))
    api(project(":compiler:resolution.common.jvm"))
    api(project(":compiler:frontend.common.jvm"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.asm)
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

