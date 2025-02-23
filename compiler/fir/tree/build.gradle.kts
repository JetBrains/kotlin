plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:frontend.common"))
    api(project(":core:compiler.common"))
    api(project(":compiler:fir:cones"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:util"))
    implementation(project(":core:metadata"))

    if (kotlinBuildProperties.isInIdeaSync) {
        compileOnly(project("tree-generator")) // Provided, so that IDEA can recognize references to this module in KDoc.
    }

    // Necessary only to store bound PsiElement inside FirElement
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}

generatedSourcesTask(
    taskName = "generateTree",
    generatorProject = ":compiler:fir:tree:tree-generator",
    generatorRoot = "compiler/fir/tree/tree-generator/src/",
    generatorMainClass = "org.jetbrains.kotlin.fir.tree.generator.MainKt",
)
