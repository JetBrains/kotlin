plugins {
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-api"))
    compileOnly(libs.kotlin.compiler.before.bta.v2)
    implementation(project(":kotlin-tooling-core")) // to reuse `KotlinToolingVersion`

    testCompileOnly(project(":compiler:build-tools:kotlin-build-tools-api"))
    testImplementation(kotlinTest("junit"))
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler {
    from(mainSourceSet.output)
})
sourcesJar()
javadocJar()

kotlin {
    explicitApi()
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi")
    }
}

generatedSourcesTask(
    taskName = "generateBtaSources",
    generatorProject = ":compiler:build-tools:kotlin-build-tools-generator",
    generatorMainClass = "org.jetbrains.kotlin.buildtools.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            "2.2.20",
            "impl",
            "jvmCompilerArguments",
            "org.jetbrains.kotlin.buildtools.internal.compat.arguments",
            "compat",
            version.toString()
        )
    }
)
