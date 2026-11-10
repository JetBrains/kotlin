plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("generated-sources")
}

dependencies {
    api(project(":core:language.version-settings"))
    api(project(":core:metadata"))
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
    api(project(":compiler:incremental-compilation-klib:api"))
    compileOnly(project(":kotlin-util-klib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

generatedConfigurationKeys("CommonConfigurationKeys", "KlibConfigurationKeys")
