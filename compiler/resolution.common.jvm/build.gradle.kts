plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:resolution.common"))
    api(project(":core:compiler.common.jvm"))
    api(project(":compiler:psi"))
    implementation(project(":compiler:util"))
    implementation(commonDependency("io.javaslang","javaslang"))
    compileOnly(intellijCore())
    compileOnly(libs.kotlinx.coroutines.core.jvm)
    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    compileOnly(commonDependency("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil"))
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
