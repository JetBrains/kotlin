import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

dependencies {
    api(project(":compiler:psi:psi-api"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":compiler:resolution.common.jvm"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:psi:psi-frontend-utils"))
    implementation(project(":compiler:psi:psi-impl"))
    implementation(project(":core:compiler.common.jvm"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":compiler:backend.jvm"))
    implementation(kotlinxCollectionsImmutable())
    api(intellijCore())
    implementation(libs.caffeine)

    testFixturesApi(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter.api)
    testFixturesImplementation(kotlinTest("junit"))
    testFixturesImplementation(project(":analysis:analysis-api"))
    testFixturesImplementation(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testFixturesImplementation(testFixtures(project(":compiler:tests-common")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure-utils")))
    testFixturesApi(testFixtures(project(":compiler:test-infrastructure")))
    testFixturesImplementation(testFixtures(project(":plugins:plugin-sandbox")))
    testFixturesImplementation(testFixtures(project(":compiler:tests-common-new")))
    testFixturesImplementation(project(":analysis:analysis-internal-utils"))
    testFixturesImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testFixturesImplementation(project(":analysis:decompiled:light-classes-for-decompiled"))
    testFixturesImplementation(project(":analysis:decompiled:decompiler-native"))
    testFixturesImplementation(project(":kotlin-util-klib-metadata"))
    testFixturesImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testFixturesCompileOnly(toolsJarApi())

    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
    testImplementation(testFixtures(project(":compiler:tests-common")))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
            "org.jetbrains.kotlin.analysis.api.KaIdeApi",
            "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
            "org.jetbrains.kotlin.analysis.api.KaContextParameterApi",
            "org.jetbrains.kotlin.analysis.api.components.KaSessionComponentImplementationDetail",
            "org.jetbrains.kotlin.analysis.api.KaSpiExtensionPoint",
        )
    )
}

projectTests {
    testCodebaseTask(dumpDirs = emptyList()) {
        // Forward the source-code-update flag (used by the `analysis-api-mark-internal-apis` skill) from a Gradle property to the test
        // JVM. Combine with `-Pkotlin.test.instrumentation.disable.inputs.check=true` so the test can write to source files.
        val updateSourceCode = "kotlin.analysis.codebaseTest.internalApi.updateSourceCode"
        systemProperty(updateSourceCode, project.providers.gradleProperty(updateSourceCode).orElse("false").get())
    }
}
