plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("generated-sources")
}

dependencies {
    api(project(":core:language.version-settings"))
    api(project(":core:metadata"))
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
    compileOnly(project(":kotlin-util-klib"))
    compileOnly(intellijCore())
    implementation(libs.androidx.tracing.core)
    implementation(libs.androidx.tracing.wire)
    implementation(libs.okio)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

generatedConfigurationKeys("CommonConfigurationKeys", "KlibConfigurationKeys")
