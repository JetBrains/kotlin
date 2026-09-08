plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":core:language.version-settings"))
    implementation(project(":generators"))
    implementation(project(":compiler:arguments"))
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
