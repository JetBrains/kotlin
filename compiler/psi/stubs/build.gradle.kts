plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-inputs-check")
}

// Distinguishes the coordinates from ':analysis:stubs', which has the same project name
group = "org.jetbrains.kotlin.psi"

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":core:compiler.common"))
    api(project(":core:metadata"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:psi:parser"))
    implementation(project(":core:error.handling"))
    implementation(project(":core:util.runtime"))

    compileOnly(intellijCore())
    compileOnly(libs.org.jetbrains.annotations)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
    testFixturesCompileOnly(intellijCore())
    testCompileOnly(intellijCore())
}

kotlin {
    // The stub implementation builds the PSI from stubs, so it opts in to all of psi-api's non-public markers
    // (the same list psi-api excludes from its ABI dump) instead of repeating the suppression in every file.
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.psi.KtImplementationDetail",
            "org.jetbrains.kotlin.psi.KtNonPublicApi",
            "org.jetbrains.kotlin.psi.KtIdeApi",
            "org.jetbrains.kotlin.psi.KtExperimentalApi",
            "org.jetbrains.kotlin.psi.KtPlatformInterface",
        )
    )
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

projectTests {
    testCodebaseTask()
}
