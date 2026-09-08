plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:frontend.common"))
    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:psi:psi-impl"))
    api(project(":compiler:psi:psi-frontend-utils"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

generatedSourcesTask(
    taskName = "generateFeatureToFlagMap",
    generatorProject = ":compiler:frontend.common-psi:feature-to-flag-map-generator",
    generatorMainClass = "org.jetbrains.kotlin.diagnostics.rendering.generator.FeatureToFlagMapGeneratorKt",
)
