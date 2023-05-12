plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-barebone"))
    api(project(":analysis:kt-references"))
    api(intellijCore())
    implementation(project(":analysis:analysis-internal-utils"))

    testApiJUnit5()
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testApi(project(":analysis:analysis-api"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":analysis:analysis-api-impl-barebone"))
    testApi(project(":analysis:symbol-light-classes"))
    testApi(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
    testApi(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testApi(project(":analysis:decompiled:decompiler-to-psi"))
    testApi(projectTests(":analysis:analysis-test-framework"))
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testImplementation(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
    kotlinOptions.freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals"
}

testsJar()
