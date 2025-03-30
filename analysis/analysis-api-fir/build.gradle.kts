import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:checkers:checkers.native"))
    api(project(":compiler:fir:fir-jvm"))
    api(project(":compiler:fir:entrypoint"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":analysis:light-classes-base"))
    api(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.jvm"))
    api(intellijCore())
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:kt-references"))
    implementation(project(":analysis:symbol-light-classes"))
    implementation(libs.caffeine)
    implementation(libs.opentelemetry.api)

    testImplementation(projectTests(":analysis:low-level-api-fir"))
    testImplementation(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testImplementation(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(project(":analysis:decompiled:decompiler-to-psi"))
    testImplementation(kotlinTest("junit"))
    testApi(projectTests(":analysis:analysis-test-framework"))

    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

optInToUnsafeDuringIrConstructionAPI()

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}.also { confugureFirPluginAnnotationsDependency(it) }

testsJar()

allprojects {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.optIn.addAll(
            listOf(
                "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
                "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
                "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
                "org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
                "org.jetbrains.kotlin.analysis.api.KaIdeApi",
                "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
                "org.jetbrains.kotlin.analysis.api.permissions.KaAllowProhibitedAnalyzeFromWriteAction"
            )
        )
    }
}

generatedSourcesTask(
    taskName = "generateDiagnostics",
    generatorProject = ":analysis:analysis-api-fir:analysis-api-fir-generator",
    generatorRoot = "analysis/analysis-api-fir/analysis-api-fir-generator/src/",
    generatorMainClass = "org.jetbrains.kotlin.analysis.api.fir.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            "org.jetbrains.kotlin.analysis.api.fir.diagnostics",
            generationRoot.toString(),
        )
    }
)
