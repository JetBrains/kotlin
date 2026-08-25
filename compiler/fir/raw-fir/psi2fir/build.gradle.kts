/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("require-explicit-types")
}

dependencies {
    api(project(":compiler:fir:raw-fir:raw-fir.common"))
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":compiler:psi:psi-frontend-utils"))

    compileOnly(intellijCore())
    compileOnly(libs.guava)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(project(":compiler:multiplatform-parsing"))
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesApi(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:tests-common-new")))
    testFixturesApi(testFixtures(project(":compiler:fir:raw-fir:raw-fir.common")))
    testFixturesApi(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    testCompileOnly(kotlinTest("junit"))
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

    testGenerator("org.jetbrains.kotlin.fir.builder.TestGeneratorForPsi2FirKt")
}

testsJar()
