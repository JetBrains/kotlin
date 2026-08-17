import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("jacoco-report-aggregation")
}

description = "Aggregated test coverage report for the compiler frontend (FIR) area (KQA-3084)"

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

dependencies {
    // Execution data producers: the test tasks agreed on in KQA-3083. Their `.exec` files flow
    // in through the `coverageDataElementsForTest` variants that appear when
    // `test-coverage-convention` applies JaCoCo (only with `kotlin.build.coverage.enabled`).
    jacocoAggregation(project(":compiler"))
    jacocoAggregation(project(":compiler:tests-integration"))
    jacocoAggregation(project(":compiler:fir:analysis-tests"))
    jacocoAggregation(project(":compiler:fir:fir2ir"))
    jacocoAggregation(project(":compiler:fir:raw-fir:psi2fir"))
    jacocoAggregation(project(":compiler:fir:raw-fir:light-tree2fir"))

    // FIR production modules: provide the classes and sources the report is calculated for.
    // The test-owning projects above have empty `main` source sets, so nothing flows in
    // transitively — the report scope is declared explicitly and deterministically here.
    jacocoAggregation(project(":compiler:fir:cones"))
    jacocoAggregation(project(":compiler:fir:tree"))
    jacocoAggregation(project(":compiler:fir:providers"))
    jacocoAggregation(project(":compiler:fir:semantics"))
    jacocoAggregation(project(":compiler:fir:resolve"))
    jacocoAggregation(project(":compiler:fir:plugin-utils"))
    jacocoAggregation(project(":compiler:fir:fir-serialization"))
    jacocoAggregation(project(":compiler:fir:fir-deserialization"))
    jacocoAggregation(project(":compiler:fir:fir-jvm"))
    jacocoAggregation(project(":compiler:fir:fir-js"))
    jacocoAggregation(project(":compiler:fir:fir-native"))
    jacocoAggregation(project(":compiler:fir:checkers"))
    jacocoAggregation(project(":compiler:fir:checkers:checkers.jvm"))
    jacocoAggregation(project(":compiler:fir:checkers:checkers.js"))
    jacocoAggregation(project(":compiler:fir:checkers:checkers.native"))
    jacocoAggregation(project(":compiler:fir:checkers:checkers.wasm"))
    jacocoAggregation(project(":compiler:fir:checkers:checkers.web.common"))
    jacocoAggregation(project(":compiler:fir:diagnostic-renderers"))
    jacocoAggregation(project(":compiler:fir:entrypoint"))
    jacocoAggregation(project(":compiler:fir:raw-fir:raw-fir.common"))
    jacocoAggregation(project(":compiler:fir:fir2ir:jvm-backend"))
}

reporting {
    reports {
        register<JacocoCoverageReport>("firCoverageReport") {
            testSuiteName = "test"
        }
    }
}

tasks.named<JacocoReport>("firCoverageReport") {
    group = "verification"
    description = "Generates an aggregated coverage report for FIR classes from the KQA-3083 test set"

    reports {
        html.required = true
        xml.required = true
        csv.required = false
    }

    // Scope the report strictly to FIR frontend classes. Snapshot the plugin-wired dirs first
    // to avoid a self-referencing file collection; `matching` keeps the filtering lazy and
    // configuration-cache-compatible (no Project/script references at execution time).
    val wiredClassDirs = objects.fileCollection().from(classDirectories.from.toList())
    classDirectories.setFrom(wiredClassDirs.asFileTree.matching { include("org/jetbrains/kotlin/fir/**") })

    // Tolerate producers that didn't run (e.g. excluded with `-x`): report on the data that exists.
    val wiredExecData = objects.fileCollection().from(executionData.from.toList())
    executionData.setFrom(wiredExecData.filter { it.exists() })

    onlyIf("No JaCoCo execution data found — run test tasks with -Pkotlin.build.coverage.enabled=true") {
        executionData.files.isNotEmpty()
    }
}
