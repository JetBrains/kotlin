plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("gradle-plugin-compiler-dependency-configuration")
    id("generated-sources")
}

dependencies {
    api(project(":core:util.runtime"))
    api(project(":compiler:arguments.common"))
    implementation(project(":compiler:config.jvm"))
    implementation(project(":js:js.config"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":native:kotlin-native-utils"))
    api(project(":compiler:plugin-api"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(intellijCore())
    compileOnly(libs.guava)
    compileOnly(libs.intellij.asm)
}

sourceSets {
    "main" {
        projectDefault()
        generatedDir()
    }
    "test" {}
}

optInToExperimentalCompilerApi()

tasks.getByName<Jar>("jar") {
    //excludes unused bunch files
    exclude("META-INF/extensions/*.xml.**")
}

generatedSourcesTask(
    taskName = "generateCliArguments",
    generatorProject = ":compiler:cli:cli-arguments-generator",
    generatorRoot = "compiler/cli/cli-argument-generator/src",
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
            "nativeKlibArguments",
            "nativeArguments",
            "metadataArguments",
        )
    }
)
