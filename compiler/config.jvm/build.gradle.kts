plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":compiler:config"))
    api(project(":core:compiler.common.jvm"))
    compileOnly(libs.intellij.asm)
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
