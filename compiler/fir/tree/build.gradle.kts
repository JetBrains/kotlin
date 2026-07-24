plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
    id("require-explicit-types")
}

dependencies {
    implementation(project(":compiler:container"))
    api(project(":compiler:frontend.common"))
    api(project(":core:compiler.common"))
    api(project(":compiler:fir:cones"))

    if (kotlinBuildProperties.isInIdeaSync.get()) {
        compileOnly(project("tree-generator")) // Provided, so that IDEA can recognize references to this module in KDoc.
    }

    // Necessary only to store bound PsiElement inside FirElement
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
}

kotlin {
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
            "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
            "org.jetbrains.kotlin.types.model.K2Only",
        )
    )
}

generatedSourcesTask(
    taskName = "generateTree",
    generatorProject = ":compiler:fir:tree:tree-generator",
    generatorMainClass = "org.jetbrains.kotlin.fir.tree.generator.MainKt",
)
