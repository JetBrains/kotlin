plugins {
    kotlin("jvm")
}

tasks.register("analysisAllTests") {
    dependsOn(
        ":analysis:decompiled:decompiler-to-file-stubs:test",
        ":analysis:decompiled:decompiler-to-psi:test",
        ":analysis:decompiled:native:test",
        ":analysis:analysis-api:test",
        ":analysis:analysis-api-fir:test",
        ":analysis:analysis-api-fe10:test",
        ":analysis:analysis-api-standalone:test",
        ":analysis:low-level-api-fir:test",
        ":analysis:symbol-light-classes:test"
    )
}
