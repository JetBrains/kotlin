import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(kotlinStdlib())
    testFixturesImplementation(intellijCore())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)

    testFixturesImplementation(kotlinTest("junit"))
    testFixturesImplementation(project(":compiler:psi:psi-api"))
    testFixturesApi(testFixtures(project(":compiler:psi:psi-api")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesImplementation(project(":analysis:analysis-api-platform-interface"))
    testFixturesImplementation(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testFixturesImplementation(project(":analysis:analysis-api-impl-base"))
    testFixturesImplementation(project(":analysis:decompiled:decompiler-to-psi"))
    testFixturesImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testFixturesImplementation(project(":wasm:wasm.frontend"))
    testFixturesApi(testFixtures(project(":analysis:test-data-manager")))
}

sourceSets {
    "main" { none() }
    "test" { none() }
    "testFixtures" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
        "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
        "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
        "org.jetbrains.kotlin.analysis.api.KaContextParameterApi",
        "org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint",
    )
}
