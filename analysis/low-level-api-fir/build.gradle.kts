import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

val scriptingTestDefinition by configurations.creating

dependencies {
    api(project(":compiler:psi"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:fir:fir2ir:jvm-backend"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:checkers:checkers.native"))
    api(project(":compiler:fir:checkers:checkers.wasm"))
    api(project(":compiler:fir:fir-jvm"))
    api(project(":compiler:backend.common.jvm"))
    api(project(":compiler:cli-common"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    testImplementation(project(":analysis:analysis-api-fir"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:fir:entrypoint"))
    implementation(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    implementation(project(":kotlin-scripting-compiler"))
    implementation(project(":kotlin-scripting-common"))
    implementation(project(":kotlin-assignment-compiler-plugin.k2"))
    implementation(project(":kotlin-assignment-compiler-plugin.cli"))
    implementation(libs.caffeine)

    implementation(libs.opentelemetry.api)

    api(intellijCore())

    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    testImplementation(libs.opentest4j)
    testImplementation(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":compiler:fir:raw-fir:psi2fir"))
    testImplementation(kotlinTest("junit"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(project(":analysis:symbol-light-classes"))
    testImplementation(projectTests(":plugins:scripting:scripting-tests"))
    testImplementation(project(":kotlin-scripting-common"))

    testRuntimeOnly(project(":core:descriptors.runtime"))


    // We use 'api' instead of 'implementation' because other modules might be using these jars indirectly
    testApi(project(":plugins:plugin-sandbox"))
    testApi(projectTests(":plugins:plugin-sandbox"))

    scriptingTestDefinition(projectTests(":plugins:scripting:test-script-definition"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")

        optIn.addAll(
            "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
        )
    }
}

projectTest(
    jUnitMode = JUnitMode.JUnit5,
    defineJDKEnvVariables = listOf(
        JdkMajorVersion.JDK_17_0, // TestsWithJava11 and others
        JdkMajorVersion.JDK_17_0, // TestsWithJava17 and others
        JdkMajorVersion.JDK_21_0  // TestsWithJava21 and others
    )
) {
    dependsOn(":dist", ":plugins:scripting:test-script-definition:testJar")
    workingDir = rootDir
    useJUnitPlatform()

    val scriptingTestDefinitionClasspath = scriptingTestDefinition.asPath
    doFirst {
        systemProperty("kotlin.script.test.script.definition.classpath", scriptingTestDefinitionClasspath)
    }
}

allprojects {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.optIn.addAll(
            listOf(
                "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
                "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
                "org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals",
                "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
                "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            )
        )
    }
}

testsJar()

tasks.register("analysisLowLevelApiFirAllTests") {
    dependsOn(
        ":analysis:low-level-api-fir:test",
    )

    if (kotlinBuildProperties.isKotlinNativeEnabled) {
        dependsOn(
            ":analysis:low-level-api-fir:low-level-api-fir-native:llFirNativeTests",
        )
    }
}
