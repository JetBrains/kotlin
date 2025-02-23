plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    implementation(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(project(":compiler:util"))
    implementation(project(":core:compiler.common"))
    api(project(":compiler:cli-common"))
    api(project(":kotlin-build-common"))
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
