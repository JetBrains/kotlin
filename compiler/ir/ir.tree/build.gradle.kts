import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
}

dependencies {
    api(project(":core:descriptors"))
    api(project(":core:deserialization"))
    implementation(project(":compiler:util"))

    if (kotlinBuildProperties.isInIdeaSync) {
        compileOnly(project("tree-generator")) // Provided, so that IDEA can recognize references to this module in KDoc.
    }
    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()


sourceSets {
    "main" { projectDefault() }
    "test" {}
}

tasks.withType<KotlinJvmCompile> {
    compilerOptions.freeCompilerArgs.add("-Xinline-classes")
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}

generatedSourcesTask(
    taskName = "generateTree",
    generatorProject = ":compiler:ir.tree:tree-generator",
    generatorRoot = "compiler/ir/ir.tree/tree-generator/src/",
    generatorMainClass = "org.jetbrains.kotlin.ir.generator.MainKt",
)

