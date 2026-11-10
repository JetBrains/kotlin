plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration") // via daemon-client
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":compiler:build-tools:kotlin-build-tools-api"))
    api(project(":compiler:cli-base")) { isTransitive = false }
    api(project(":compiler:util")) { isTransitive = false }
    api(project(":core:compiler.common")) { isTransitive = false }
    api(project(":core:util.runtime")) { isTransitive = false }
    api(project(":kotlin-build-common"))
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    api(kotlin("stdlib", coreDepsVersion))
    compileOnly(libs.kotlinx.serialization.protobuf)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
