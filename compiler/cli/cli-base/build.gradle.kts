plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("generated-sources")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

dependencies {
    api(project(":core:util.runtime"))
    api(project(":compiler:arguments.common"))
    api(project(":compiler:plugin-api"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:serialization"))
    implementation(project(":compiler:resolution"))
    implementation(project(":compiler:psi:parser"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:descriptors.jvm"))
    implementation(project(":core:deserialization"))
    implementation(project(":compiler:backend"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":compiler:frontend.common-psi"))
    implementation(project(":compiler:frontend.common.jvm"))
    implementation(project(":compiler:frontend.java"))
    implementation(project(":compiler:util"))
    implementation(project(":core:compiler.common.jvm"))

    implementation(project(":compiler:config.jvm"))
    implementation(project(":js:js.config"))
    implementation(project(":wasm:wasm.config"))
    implementation(project(":native:native.config"))

    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
    compileOnly(libs.intellij.asm)
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    compileOnly(libs.guava)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {
        projectDefault()
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xannotation-target-all")
    }
}

projectTests {
    testTask()
}

optInToExperimentalCompilerApi()

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
            "commonJsAndWasmArguments",
            "legacyWasmArguments",
        )
    }
)
