plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("test-data-manager")
    id("test-inputs-check")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend.common"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.fastutil)

    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:parser"))

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(libs.junit.jupiter.api)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesCompileOnly(intellijCore())
    testCompileOnly(intellijCore())
}

kotlin {
    // psi-impl implements the PSI API, so it opts in to all of its non-public markers
    // (the same list psi-api excludes from its ABI dump) instead of repeating the
    // suppression in every file.
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
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask()

    testGenerator("org.jetbrains.kotlin.TestGeneratorForPsiImplKt")

    testData(project.isolated, "testData")

    testCodebaseTask()
}
