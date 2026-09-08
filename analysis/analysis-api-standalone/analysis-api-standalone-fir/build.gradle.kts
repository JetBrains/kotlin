plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("test-inputs-check")
}

dependencies {
    implementation(intellijCore())
    implementation(kotlinStdlib())
    implementation(project(":compiler:psi:psi-api"))
    implementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:decompiled:decompiler-native"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:psi:parser"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    implementation(project(":kotlin-util-klib"))
    implementation(project(":kotlin-util-klib-metadata"))
    implementation(libs.caffeine)
    api(project(":compiler:cli-base"))
    api(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-fir"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":analysis:light-classes-base"))
    api(project(":analysis:analysis-api-platform-interface"))
    implementation(libs.intellij.patched.kotlinx.coroutines.core.jvm)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(testFixtures(project(":compiler:psi:psi-api")))
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaExperimentalApi")
        optIn.add("org.jetbrains.kotlin.analysis.api.KaPlatformInterface")
    }
}

projectTests {
    testCodebaseTask(dumpDirs = emptyList()) {
        // Forward the source-code-update flag (used by the `analysis-api-mark-internal-apis` skill) from a Gradle property to the test
        // JVM. Combine with `-Pkotlin.test.instrumentation.disable.inputs.check=true` so the test can write to source files.
        val updateSourceCode = "kotlin.analysis.codebaseTest.internalApi.updateSourceCode"
        systemProperty(updateSourceCode, project.providers.gradleProperty(updateSourceCode).orElse("false").get())
    }
}
