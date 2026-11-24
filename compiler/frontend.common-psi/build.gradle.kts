plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
    id("test-inputs-check")
}

dependencies {
    api(project(":compiler:frontend.common"))
    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:psi:psi-impl"))
    api(project(":compiler:psi:psi-frontend-utils"))
    compileOnly(intellijCore())

    testImplementation(libs.junit.jupiter.api)
    testImplementation(intellijCore())
    testRuntimeOnly(libs.intellij.fastutil)
    testRuntimeOnly(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm"))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTests {
    testTask()
}

generatedSourcesTask(
    taskName = "generateFeatureToFlagMap",
    generatorProject = ":compiler:frontend.common-psi:feature-to-flag-map-generator",
    generatorMainClass = "org.jetbrains.kotlin.diagnostics.rendering.generator.FeatureToFlagMapGeneratorKt",
)
