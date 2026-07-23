plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
}

val runtimeOnly by configurations
val compileOnly by configurations
runtimeOnly.extendsFrom(compileOnly)

dependencies {
    implementation(project(":core:language.version-settings"))
    implementation(project(":generators"))
    implementation(project(":compiler:arguments"))

    compileOnly(intellijCore())

    runtimeOnly(intellijJDom())
}

application {
    mainClass.set("org.jetbrains.kotlin.diagnostics.rendering.generator.FeatureToFlagMapGeneratorKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
