plugins {
    id("common-configuration")
    id("test-federation-convention")
    kotlin("jvm")
    id("generated-sources")
    id("test-inputs-check")
}

dependencies {
    implementation(kotlinStdlib())
    compileOnly(project(":compiler:cli"))
}

publish()

//runtimeJar(rewriteDefaultJarDepsToShadedCompiler {
//    from(mainSourceSet.output)
//})
//
//tasks.named<ShadowJar>(EMBEDDABLE_COMPILER_TASK_NAME) {
//    relocate("org.jetbrains.kotlin.scripting", "org.jetbrains.kotlin.buildtools.internal.scripting")
//    relocate("kotlin.script.experimental", "org.jetbrains.kotlin.buildtools.internal.scripting")
//
//    transform(DontIncludeResourceTransformer::class.java) {
//        resource = "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor"
//    }
//    transform(DontIncludeResourceTransformer::class.java) {
//        resource = "META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar"
//    }
//}
//
//sourcesJar()
//javadocJar()
//
//kotlin {
//    explicitApi()
//    compilerOptions {
//        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
//    }
//}
//
//generatedSourcesTask(
//    taskName = "generateBtaSources",
//    generatorProject = ":compiler:build-tools:kotlin-build-tools-generator",
//    generatorMainClass = "org.jetbrains.kotlin.buildtools.generator.MainKt",
//    argsProvider = { generationRoot ->
//        listOf(
//            generationRoot.toString(),
//            version.toString(),
//            "impl",
//            "jvmCompilerArguments,wasmArguments,jsArguments,metadataArguments",
//        )
//    },
//)
