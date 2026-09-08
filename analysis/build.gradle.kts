plugins {
    id("common-configuration")
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

/**
 * Projects whose API surface the Analysis API tracks: the API itself, its PSI foundation, the `:core` modules
 * the surface exposes, and the published artifacts that assemble them.
 *
 * Each of them collects the checks it opted into under the `checkApiSurface` and `updateApiSurface` lifecycle tasks
 * that `common-configuration` registers, so depending on those two paths needs no cross-project access and picks up
 * a newly declared check with no further wiring here.
 */
val publicApiProjects = buildSet {
    addAll(CompilerModules.analysisApiSurfaceDependencies)
    addAll(CompilerModules.analysisApiModules)
    addAll(CompilerModules.psiModules)
    addAll(CompilerModules.analysisApiArtifacts)
}

tasks.register("checkAnalysisApiSurface") {
    group = "verification"
    description = "Verifies the API surface dumps of the Analysis API against its sources"

    dependsOn(publicApiProjects.map { "$it:checkApiSurface" })
}

tasks.register("updateAnalysisApiSurface") {
    group = "verification"
    description = "Rewrites the API surface dumps of the Analysis API from its sources"

    dependsOn(publicApiProjects.map { "$it:updateApiSurface" })
}

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
