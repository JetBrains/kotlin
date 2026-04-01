import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("generated-sources")
    id("java-test-fixtures")
}

dependencies {
    api(project(":core:descriptors"))
    api(project(":core:deserialization"))
    api(project(":compiler:frontend.common"))
    implementation(project(":compiler:util"))
    implementation(project(":compiler:config"))

    if (kotlinBuildProperties.isInIdeaSync.get()) {
        compileOnly(project("tree-generator")) // Provided, so that IDEA can recognize references to this module in KDoc.
    }
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()


sourceSets {
    "main" { projectDefault() }
    "test" {}
    "testFixtures" { projectDefault() }
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}

generatedSourcesTask(
    taskName = "generateTree",
    generatorProject = ":compiler:ir.tree:tree-generator",
    generatorMainClass = "org.jetbrains.kotlin.ir.generator.MainKt",
)

