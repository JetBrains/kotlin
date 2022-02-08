plugins {
    kotlin("jvm")
}

tasks.register("analysisAllTests") {
    dependsOn(":dist")
    dependsOn(
        ":analysis:decompiled:decompiler-to-file-stubs:test",
        ":analysis:analysis-api:test",
        ":analysis:analysis-api-fir:test",
        ":analysis:analysis-api-fe10:test",
        ":analysis:low-level-api-fir:test",
        ":analysis:symbol-light-classes:test"
    )
}
