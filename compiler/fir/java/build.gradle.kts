
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common.jvm"))
    api(project(":core:metadata.jvm"))
    api(project(":compiler:config.jvm"))
    api(project(":compiler:resolution.common.jvm"))
    api(project(":compiler:frontend.common"))
    api(project(":compiler:frontend.common.jvm"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:fir-deserialization"))

    implementation(project(":core:deserialization.common.jvm"))

    compileOnly(intellijCore())
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
}


sourceSets {
    "main" { projectDefault() }
    "test" {}
}
