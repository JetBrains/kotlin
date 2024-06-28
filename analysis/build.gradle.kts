plugins {
    kotlin("jvm")
}

tasks.register("analysisAllTests") {
    dependsOn(
        ":analysis:decompiled:decompiler-to-file-stubs:test",
        ":analysis:decompiled:decompiler-to-psi:test",
        ":analysis:decompiled:decompiler-js:test",
        ":analysis:decompiled:decompiler-native:test",
        ":analysis:analysis-api:test",
        ":analysis:analysis-api-fir:test",
        ":analysis:analysis-api-fe10:test",
        ":analysis:analysis-api-standalone:test",
        ":analysis:low-level-api-fir:test",
        ":analysis:low-level-api-fir:tests-jdk11:test",
        ":analysis:symbol-light-classes:test"
    )

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        dependsOn(
            ":analysis:analysis-api-standalone:analysis-api-standalone-native:test",
            ":analysis:low-level-api-fir:low-level-api-fir-native:llFirNativeTests",
        )
    }
}
