plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
    // kotlinx-rpc implementation in remote-daemon module uses object that needs to be marked as @Serializable
    // TODO: double check if this is really necessary
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":compiler:build-tools:kotlin-build-tools-api"))
    api(project(":compiler:cli-common")) { isTransitive = false }
    api(project(":compiler:util")) { isTransitive = false }
    api(project(":core:compiler.common")) { isTransitive = false }
    api(project(":core:util.runtime")) { isTransitive = false }
    api(project(":kotlin-build-common"))
    api(kotlinStdlib())
    // kotlinx-rpc implementation in remote-daemon module uses object that needs to be marked as @Serializable
    // TODO: double check if this is really necessary
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
