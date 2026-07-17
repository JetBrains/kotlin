import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("project-tests-convention")
    id("generated-sources")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
    embedded(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
}

kotlin {
    explicitApi()
}

publish()

standardPublicJars()

tasks.compileKotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

projectTests {
    testTask()
}

generatedSourcesTask(
    taskName = "generateBtaSources",
    generatorProject = ":compiler:build-tools:kotlin-build-tools-generator",
    generatorMainClass = "org.jetbrains.kotlin.buildtools.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            version.toString(),
            "api",
            "jvmCompilerArguments,wasmArguments,jsArguments,metadataArguments",
        )
    }
)
