plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    implementation(project(":analysis:analysis-api"))
    implementation(project(":compiler:fir:tree"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":core:compiler.common"))
    implementation(project(":core:language.model"))
    implementation(project(":core:language.version-settings"))
    implementation(project(":core:metadata"))
    implementation(project(":core:names"))
    implementation(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    explicitApiWarning()

    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "org.jetbrains.kotlin.analysis.api.KaIdeApi",
            "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            "org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
        )
    )
}

generatedSourcesTask(
    taskName = "generateDiagnostics",
    generatorProject = ":analysis:analysis-api-fir-diagnostics:analysis-api-fir-diagnostics-generator",
    generatorMainClass = "org.jetbrains.kotlin.analysis.api.fir.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            "org.jetbrains.kotlin.analysis.api.fir.diagnostics",
            generationRoot.toString(),
            "api",
        )
    }
)
