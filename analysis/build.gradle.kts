plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
}

/**
 * Analysis API modules that hold nothing but tests, so they are absent from [CompilerModules.analysisApiModules].
 */
val analysisApiTestModules = listOf(
    ":analysis:analysis-test-framework",
    ":analysis:low-level-api-fir:low-level-api-fir-compiler-tests",
    ":analysis:test-data-manager",
)

/**
 * Analysis API modules that are only part of the build when Kotlin/Native is enabled.
 */
val analysisApiNativeModules = listOf(
    ":analysis:analysis-api-standalone:analysis-api-standalone-native",
    ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests",
)

val analysisApiArtifactTests = tasks.register("analysisApiArtifactTests") {
    group = "verification"
    description = "Checks the published Analysis API artifacts"

    dependsOn(CompilerModules.analysisApiArtifacts.map { "$it:check" })
}

tasks.register("analysisAllTests") {
    group = "verification"
    description = "Checks the Analysis API, its PSI foundation, and the published artifacts"

    val modules = buildList {
        addAll(CompilerModules.analysisApiModules)
        addAll(CompilerModules.psiModules)
        addAll(analysisApiTestModules)

        if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
            addAll(analysisApiNativeModules)
        }
    }

    dependsOn(analysisApiArtifactTests)
    dependsOn(modules.map { "$it:check" })
}
