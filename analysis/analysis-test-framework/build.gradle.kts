import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("java-test-fixtures")
}

dependencies {
    testFixturesImplementation(kotlinStdlib())
    testFixturesImplementation(intellijCore())
    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)

    testFixturesImplementation(kotlinTest("junit"))
    testFixturesImplementation(project(":analysis:analysis-internal-utils"))
    testFixturesImplementation(project(":compiler:psi:psi-api"))
    testFixturesImplementation(project(":analysis:kt-references"))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesImplementation(project(":analysis:analysis-api-platform-interface"))
    testFixturesImplementation(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testFixturesApi(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testFixturesImplementation(project(":analysis:analysis-api-impl-base"))
    testFixturesImplementation(project(":analysis:decompiled:decompiler-to-psi"))
    testFixturesImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
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
    )
}
