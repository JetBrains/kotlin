plugins {
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:frontend.common"))
    api(project(":core:compiler.common"))
    api(project(":compiler:fir:cones"))

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
    generatorMainClass = "org.jetbrains.kotlin.fir.tree.generator.MainKt",
)
