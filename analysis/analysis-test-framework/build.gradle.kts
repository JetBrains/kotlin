import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    testImplementation(kotlinStdlib())
    testImplementation(intellijCore())
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(kotlinTest("junit"))
    testImplementation(project(":analysis:analysis-internal-utils"))
    testImplementation(project(":compiler:psi"))
    testImplementation(project(":analysis:kt-references"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:tests-common"))
    testImplementation(project(":analysis:analysis-api-platform-interface"))
    testImplementation(project(":analysis:analysis-api"))
    testApi(project(":analysis:analysis-api-standalone:analysis-api-standalone-base"))
    testApi(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base"))
    testImplementation(project(":analysis:analysis-api-impl-base"))
    testImplementation(project(":analysis:decompiled:decompiler-to-psi"))
    testImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
}

sourceSets {
    "main" { none() }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":plugins:plugin-sandbox:plugin-annotations:jar")
    workingDir = rootDir
    useJUnitPlatform()
}

testsJar()

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")

    compilerOptions.optIn.addAll(
        "org.jetbrains.kotlin.analysis.api.KaExperimentalApi",
        "org.jetbrains.kotlin.analysis.api.KaPlatformInterface",
    )
}
