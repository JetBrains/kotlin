import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    api(project(":analysis:kt-references"))
    api(project(":compiler:resolution.common.jvm"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":compiler:backend-common"))
    implementation(kotlinxCollectionsImmutable())
    api(intellijCore())
    implementation(project(":analysis:analysis-internal-utils"))

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(kotlinTest("junit"))
    testImplementation(project(":analysis:analysis-api"))
    testImplementation(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testImplementation(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":plugins:plugin-sandbox"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(project(":analysis:symbol-light-classes"))
    testImplementation(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(project(":analysis:decompiled:light-classes-for-decompiled"))
    testImplementation(project(":analysis:decompiled:decompiler-native"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
    compilerOptions.optIn.addAll(
        listOf(
            "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
            "org.jetbrains.kotlin.analysis.api.KaNonPublicApi",
            "org.jetbrains.kotlin.analysis.api.KaIdeApi"
        )
    )
}

testsJar()
