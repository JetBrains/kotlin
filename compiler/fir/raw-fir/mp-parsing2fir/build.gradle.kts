plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("require-explicit-types")
}

group = "org.jetbrains.kotlin.fir"

dependencies {
    api(project(":compiler:fir:raw-fir:raw-fir.common"))
    implementation(project(":compiler:fir:raw-fir:light-tree2fir"))
    implementation(project(":compiler:multiplatform-parsing"))
    implementation(libs.org.jetbrains.syntax.api)

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:fir:raw-fir:psi2fir")))
    testFixturesApi(testFixtures(project(":compiler:fir:raw-fir:raw-fir.common")))

    testCompileOnly(kotlinTest("junit"))

    testFixturesCompileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

kotlin {
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.fir.symbols.SymbolInternals",
            "org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess",
            "org.jetbrains.kotlin.types.model.K2Only",
        )
    )
}

projectTests {
    testTask {
        workingDir = rootDir
    }

    testGenerator("org.jetbrains.kotlin.fir.builder.TestGeneratorForMultiplatformParsing2FirKt")
}

testsJar()
