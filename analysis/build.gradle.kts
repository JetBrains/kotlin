tasks.register("analysisAllTests") {
    dependsOn(
        ":analysis:analysis-api-fe10:test",
        ":analysis:analysis-api-fir:test",
        ":analysis:analysis-api-platform-interface:checkKotlinAbi",
        ":analysis:analysis-api-standalone:checkKotlinAbi",
        ":analysis:analysis-api-standalone:test",
        ":analysis:analysis-api:checkKotlinAbi",
        ":analysis:analysis-api:test",
        ":analysis:decompiled:decompiler-js:test",
        ":analysis:decompiled:decompiler-native:test",
        ":analysis:decompiled:decompiler-to-file-stubs:test",
        ":analysis:decompiled:decompiler-to-psi:test",
        ":analysis:low-level-api-fir:test",
        ":analysis:low-level-api-fir:tests-jdk11:test",
        ":analysis:low-level-api-fir:low-level-api-fir-compiler-tests:test",
        ":analysis:stubs:test",
        ":analysis:symbol-light-classes:test",
        ":analysis:test-data-manager:test",
        ":compiler:psi:psi-api:checkKotlinAbi",
        ":compiler:psi:psi-api:test",
    )

    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        dependsOn(
            ":analysis:analysis-api-standalone:analysis-api-standalone-native:test",
            ":analysis:low-level-api-fir:low-level-api-fir-native-compiler-tests:llFirNativeTests",
        )
    }
}
