import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("test-inputs-check")
}

dependencies {
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:decompiled:decompiler-native"))
    implementation(intellijCore())
    implementation(libs.opentelemetry.api)
    implementation(libs.caffeine)

    testFixturesApi(kotlinTest("junit"))
    testFixturesApi(platform(libs.junit.bom))
    testFixturesApi(libs.junit.jupiter.api)
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
}

kotlin {
    explicitApi()

    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaPlatformInterface")
    }

    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
        legacyDump.referenceDumpDir = File("api-unstable")

        filters {
            exclude.annotatedWith.addAll(
                "org.jetbrains.kotlin.analysis.api.KaImplementationDetail",
            )
        }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
    "codebaseTest" {
        java.srcDirs("codebaseTest")
        compileClasspath += configurations["testCompileClasspath"]
        runtimeClasspath += configurations["testRuntimeClasspath"]
    }
}

projectTests {
    testTask(taskName = "testCodebase", jUnitMode = JUnitMode.JUnit5, skipInLocalBuild = false) {
        group = "verification"

        classpath += sourceSets.getByName("codebaseTest").runtimeClasspath
        testClassesDirs = sourceSets.getByName("codebaseTest").output.classesDirs
    }

    testData(project.isolated, "src")
    testData(project.isolated, "api")
    testData(project.isolated, "api-unstable")
}

tasks.named("check") {
    dependsOn("testCodebase")
}