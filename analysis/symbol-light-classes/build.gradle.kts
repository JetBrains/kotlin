plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":compiler:psi"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":core:compiler.common"))
    implementation(project(":analysis:light-classes-base"))
    implementation(project(":compiler:backend.common.jvm"))
    implementation(project(":analysis:analysis-api-providers"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:project-structure"))
    implementation(project(":analysis:decompiled:light-classes-for-decompiled"))
    implementation(intellijCore())
    implementation(kotlinxCollectionsImmutable())

    testImplementation(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:analysis-api-fir"))
    testImplementation(projectTests(":compiler:tests-common-new"))
    testImplementation(projectTests(":analysis:low-level-api-fir"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
    kotlinOptions.freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.analysis.api.lifetime.KtAllowProhibitedAnalyzeFromWriteAction"
}


testsJar()
