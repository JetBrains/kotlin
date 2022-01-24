plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:psi"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:ir.tree"))
    api(project(":compiler:fir:resolve"))
    api(project(":compiler:fir:providers"))
    api(project(":compiler:fir:semantics"))
    api(project(":compiler:fir:checkers"))
    api(project(":compiler:fir:checkers:checkers.jvm"))
    api(project(":compiler:fir:checkers:checkers.js"))
    api(project(":compiler:fir:java"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":compiler:light-classes"))
    api(intellijCore())
    implementation(project(":analysis:analysis-api-providers"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":analysis:kt-references"))

    testApi(projectTests(":analysis:low-level-api-fir"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testApi(projectTests(":analysis:analysis-api-impl-base"))
    testApi(projectTests(":analysis:decompiled:decompiler-to-file-stubs"))
    testApi(project(":analysis:decompiled:decompiler-to-file-stubs"))
    testApi(project(":analysis:decompiled:decompiler-to-psi"))
    testApi(project(":kotlin-test:kotlin-test-junit"))

    testApi(toolsJar())
    testApiJUnit5()
    testApi(project(":analysis:symbol-light-classes"))

    // We use 'api' instead of 'implementation' because other modules might be using these jars indirectly
    testApi(project(":plugins:fir-plugin-prototype"))
    testApi(projectTests(":plugins:fir-plugin-prototype"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()

    // PluginAnnotationsProvider needs this jar during tests
    dependsOn(":plugins:fir-plugin-prototype:plugin-annotations:jar")
}

testsJar()

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
        kotlinOptions {
            freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.fir.symbols.SymbolInternals"
        }
    }
}

val generatorClasspath by configurations.creating

dependencies {
    generatorClasspath(project(":analysis:analysis-api-fir:analysis-api-fir-generator"))
}

val generateCode by tasks.registering(NoDebugJavaExec::class) {
    val generatorRoot = "$projectDir/analysis/analysis-api-fir/analysis-api-fir-generator/src/"

    val generatorConfigurationFiles = fileTree(generatorRoot) {
        include("**/*.kt")
    }

    inputs.files(generatorConfigurationFiles)

    workingDir = rootDir
    classpath = generatorClasspath
    main = "org.jetbrains.kotlin.analysis.api.fir.generator.MainKt"
    systemProperties["line.separator"] = "\n"
}

val compileKotlin by tasks

compileKotlin.dependsOn(generateCode)
