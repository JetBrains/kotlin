plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("generated-sources")
}

dependencies {
    api(project(":core:util.runtime"))
    api(project(":compiler:arguments.common"))
    api(project(":compiler:plugin-api"))
    api(project(":compiler:resolution.common"))
    api(project(":compiler:light-classes"))

    implementation(project(":compiler:config.jvm"))
    implementation(project(":js:js.config"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":native:native.config"))
    api(project(":compiler:plugin-api"))
    implementation(project(":kotlin-util-klib-metadata"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.intellij.asm)
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    compileOnly(libs.guava)
    runtimeOnly(libs.kotlinx.coroutines.core)
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" { none() }
}

optInToExperimentalCompilerApi()
optInToK1Deprecation()

tasks.jar.configure {
    //excludes unused bunch files
    exclude("META-INF/extensions/*.xml.**")
}

generatedConfigurationKeys("CLIConfigurationKeys")

generatedSourcesTask(
    taskName = "generateCliArguments",
    generatorProject = ":compiler:cli:cli-arguments-generator",
    generatorMainClass = "org.jetbrains.kotlin.cli.arguments.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            "commonToolArguments",
            "commonCompilerArguments",
            "jvmCompilerArguments",
            "commonKlibBasedArguments",
            "wasmArguments",
            "jsArguments",
            "nativeArguments",
            "metadataArguments",
        )
    }
)
