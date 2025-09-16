plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    api(kotlin("stdlib", coreDepsVersion))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
