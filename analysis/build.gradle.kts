tasks.register("analysisAllTests") {
    dependsOn(
        ":analysis:analysis-api:check",
        ":analysis:analysis-api-fe10:check",
        ":analysis:analysis-api-fir:check",
        ":analysis:analysis-api-platform-interface:check",
        ":analysis:analysis-api-standalone:check",
        ":analysis:decompiled:decompiler-js:check",
        ":analysis:decompiled:decompiler-native:check",
        ":analysis:decompiled:decompiler-to-file-stubs:check",
        ":analysis:decompiled:decompiler-to-psi:check",
        ":analysis:kt-references:check",
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
