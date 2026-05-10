plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
    compileOnly(libs.intellij.asm)
}

sourceSets {
    "main" { projectDefault() }
}
