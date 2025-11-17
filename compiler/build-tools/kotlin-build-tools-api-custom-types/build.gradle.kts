plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
}

kotlin {
    explicitApi()
}

tasks.compileKotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}