plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("require-explicit-types")
}

group = "org.jetbrains.kotlin.fir"

dependencies {
    api(project(":compiler:fir:raw-fir:raw-fir.common"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":compiler:psi:parser"))

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

    testGenerator("org.jetbrains.kotlin.fir.lightTree.TestGeneratorForLightTree2FirKt")
}

testsJar()
