
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
    api(project(":compiler:fir:java"))
    api(project(":analysis:low-level-api-fir"))
    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-impl-base"))
    api(project(":compiler:light-classes"))
    api(intellijCoreDep())
    implementation(project(":analysis:analysis-api-providers"))

    testApi(projectTests(":analysis:low-level-api-fir"))
    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler:test-infrastructure-utils"))
    testApi(projectTests(":compiler:test-infrastructure"))
    testApi(projectTests(":compiler:tests-common-new"))
    testApi(projectTests(":compiler:fir:analysis-tests:legacy-fir-tests"))
    testApi(projectTests(":analysis:analysis-api-impl-base"))
    testApi(project(":kotlin-test:kotlin-test-junit"))
    testApi(toolsJar())
    testApiJUnit5()
    testRuntimeOnly(project(":analysis:symbol-light-classes"))

    testRuntimeOnly(intellijDep()) {
        includeJars(
            "jps-model",
            "extensions",
            "util",
            "platform-api",
            "platform-impl",
            "idea",
            "guava",
            "trove4j",
            "asm-all",
            "log4j",
            "jdom",
            "streamex",
            "bootstrap",
            "jna",
            rootProject = rootProject
        )
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(jUnit5Enabled = true) {
    dependsOn(":dist")
    workingDir = rootDir
    useJUnitPlatform()
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


