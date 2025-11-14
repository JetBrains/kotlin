import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.DontIncludeResourceTransformer

plugins {
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    implementation(kotlinStdlib())
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-js"))
    compileOnly(project(":kotlin-build-common"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":kotlin-daemon-client"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":kotlin-compiler-runner-unshaded"))
    implementation(project(":compiler:build-tools:kotlin-build-tools-cri-impl"))
    compileOnly(intellijCore())
    compileOnly(project(":kotlin-scripting-compiler"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }

    runtimeOnly(project(":kotlin-compiler-embeddable"))
    runtimeOnly(project(":kotlin-compiler-runner"))
    embedded(project(":kotlin-scripting-compiler-embeddable")) { isTransitive = false }
    embedded(project(":kotlin-scripting-compiler-impl-embeddable")) { isTransitive = false }
    embedded(project(":kotlin-scripting-common")) { isTransitive = false }
    embedded(project(":kotlin-scripting-jvm")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler {
    from(mainSourceSet.output)
})

tasks.named<ShadowJar>(EMBEDDABLE_COMPILER_TASK_NAME) {
    relocate("org.jetbrains.kotlin.scripting", "org.jetbrains.kotlin.buildtools.internal.scripting")
    relocate("kotlin.script.experimental", "org.jetbrains.kotlin.buildtools.internal.scripting")

    transform(DontIncludeResourceTransformer::class.java) {
        resource = "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor"
    }
    transform(DontIncludeResourceTransformer::class.java) {
        resource = "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar"
    }
    transform(DontIncludeResourceTransformer::class.java) {
        resource = "META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar"
    }
}

sourcesJar()
javadocJar()

kotlin {
    explicitApi()
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

generatedSourcesTask(
    taskName = "generateBtaArguments",
    generatorProject = ":compiler:build-tools:kotlin-build-tools-options-generator",
    generatorMainClass = "org.jetbrains.kotlin.buildtools.options.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            version.toString(),
            "impl",
            "jvmCompilerArguments",
        )
    },
)
