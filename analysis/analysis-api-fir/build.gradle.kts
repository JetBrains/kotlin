plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:checkers:checkers.native"))
    api(project(":compiler:fir:java"))
    api(project(":compiler:fir:entrypoint"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":analysis:light-classes-base"))
    api(project(":compiler:backend.common.jvm"))
    implementation(project(":compiler:cli-base"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:backend.jvm.entrypoint"))
    implementation(project(":compiler:backend.jvm.lower"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.serialization.jvm"))
    api(intellijCore())
    implementation(project(":analysis:analysis-api-providers"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:kt-references"))

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
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testApi(projectTests(":analysis:analysis-test-framework"))

    testImplementation(toolsJar())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(project(":analysis:symbol-light-classes"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}.also { confugureFirPluginAnnotationsDependency(it) }

testsJar()

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.fir.symbols.SymbolInternals"
            freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.analysis.api.lifetime.KtAllowProhibitedAnalyzeFromWriteAction"
        }
    }
}

val generatorClasspath by configurations.creating

dependencies {
    implementation(project(":compiler:fir:fir-serialization"))
    implementation(project(":compiler:backend"))
    generatorClasspath(project(":analysis:analysis-api-fir:analysis-api-fir-generator"))
}

val generateCode by tasks.registering(NoDebugJavaExec::class) {
    val generatorRoot = "$projectDir/analysis/analysis-api-fir/analysis-api-fir-generator/src/"

    val generatorConfigurationFiles = fileTree(generatorRoot) {
        include("**/*.kt")
    }

    inputs.files(generatorConfigurationFiles)

    workingDir = rootDir
    classpath = generatorClasspath
    mainClass.set("org.jetbrains.kotlin.analysis.api.fir.generator.MainKt")
    systemProperties["line.separator"] = "\n"
}

val compileKotlin by tasks

compileKotlin.dependsOn(generateCode)

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
    kotlinOptions.freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals"
}
