plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
}

dependencies {
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-api"))
    compileOnly(kotlinStdlib())
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-js"))
    compileOnly(project(":kotlin-build-common"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":kotlin-daemon-client"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":kotlin-compiler-runner-unshaded"))
    compileOnly(intellijCore())
    compileOnly(project(":kotlin-scripting-compiler"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
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
    taskName = "generateBtaArguments",
    generatorProject = ":compiler:build-tools:kotlin-build-tools-options-generator",
    generatorRoot = "compiler/build-tools/kotlin-build-tools-options-generator/src",
    generatorMainClass = "org.jetbrains.kotlin.buildtools.options.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            "2.2.20",
            "impl",
            "jvmCompilerArguments",
            "org.jetbrains.kotlin.buildtools.internal.compat.arguments",
            "compat",
        )
    }
)