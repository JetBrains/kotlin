plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-published-compiler-dependency-configuration") // via daemon-client
}

dependencies {
    implementation(project(":compiler:build-tools:kotlin-build-tools-api"))
    api(project(":compiler:cli-common")) { isTransitive = false }
    api(project(":compiler:util")) { isTransitive = false }
    api(project(":core:compiler.common")) { isTransitive = false }
    api(project(":core:util.runtime")) { isTransitive = false }
    api(project(":kotlin-build-common"))
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
