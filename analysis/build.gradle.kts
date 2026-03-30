tasks.register("analysisApiArtifactTests") {
    group = "verification"

    @Suppress("UNCHECKED_CAST")
    val analysisApiProjects = rootProject.extra["analysisApiArtifacts"] as List<String>

    val analysisApiProjectChecks = analysisApiProjects
        .map { "$it:check" }

    dependsOn(*analysisApiProjectChecks.toTypedArray())
}

tasks.register("analysisAllTests") {
    group = "verification"

    dependsOn(
        ":analysis:analysisApiArtifactTests",
        ":analysis:analysis-api:check",
        ":analysis:analysis-api-fir:check",
        ":analysis:analysis-api-impl-base:check",
        ":analysis:analysis-api-platform-interface:check",
        ":analysis:analysis-api-standalone:check",
        ":analysis:decompiled:decompiler-js:check",
        ":analysis:decompiled:decompiler-native:check",
        ":analysis:decompiled:decompiler-to-file-stubs:check",
        ":analysis:decompiled:decompiler-to-psi:check",
        ":analysis:low-level-api-fir:check",
        ":analysis:low-level-api-fir:tests-jdk11:check",
        ":analysis:low-level-api-fir:low-level-api-fir-compiler-tests:check",
        ":analysis:stubs:check",
        ":analysis:symbol-light-classes:check",
        ":analysis:test-data-manager:check",
        ":compiler:psi:psi-api:check",
        ":compiler:psi:psi-impl:check",
        ":compiler:psi:psi-utils:check",
        ":compiler:psi:psi-frontend-utils:check",
    )

    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        dependsOn(
            ":analysis:analysis-api-standalone:analysis-api-standalone-native:check",
            ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests:check",
        )
    }
}
