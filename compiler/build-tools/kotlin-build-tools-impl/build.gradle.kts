import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.DontIncludeResourceTransformer
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.nativeDistribution.registerNativeBootstrapDistribution

plugins {
    id("common-configuration")
    kotlin("jvm")
    id("generated-sources")
    id("test-inputs-check")
    id("project-tests-convention")
    id("native-bootstrap-distribution-provisioner")
}

dependencies {
    api(project(":compiler:build-tools:kotlin-build-tools-api"))
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-api-jps"))
    implementation(kotlinStdlib())
    compileOnly(libs.guava)
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:cli-jvm"))
    compileOnly(project(":compiler:cli-js"))
    compileOnly(project(":compiler:cli-metadata"))
    compileOnly(project(":kotlin-build-common"))
    compileOnly(project(":daemon-common"))
    compileOnly(project(":kotlin-daemon-client"))
    compileOnly(project(":compiler:incremental-compilation-impl"))
    compileOnly(project(":kotlin-compiler-runner-unshaded"))
    implementation(project(":compiler:build-tools:kotlin-build-tools-cri-impl"))
    compileOnly(intellijCore())
    compileOnly(project(":kotlin-scripting-compiler"))
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    implementation(project(":kotlin-tooling-core"))

    runtimeOnly(project(":kotlin-compiler-embeddable"))
    runtimeOnly(project(":kotlin-compiler-runner"))

    compileOnly(project(":js:typescript-export-standalone"))
    runtimeOnly(project(":js:typescript-export-standalone-embeddable"))

    embedded(libs.guava)
    embedded(project(":kotlin-scripting-compiler-embeddable")) { isTransitive = false }
    embedded(project(":kotlin-scripting-compiler-impl-embeddable")) { isTransitive = false }
    embedded(project(":kotlin-scripting-common")) { isTransitive = false }
    embedded(project(":kotlin-scripting-jvm")) { isTransitive = false }

    // dependencies for ABI validation
    compileOnly(project(":libraries:tools:abi-validation:abi-tools-api"))
    embedded(project(":libraries:tools:abi-validation:abi-tools-api")) { isTransitive = false }
    embedded(project(":libraries:tools:abi-validation:abi-tools")) { isTransitive = false }
    embedded(project(":kotlin-metadata-jvm")) { isTransitive = false }
    embedded(libs.diff.utils) { isTransitive = false }

    if (kotlinBuildProperties.isKotlinNativeEnabled.get()) {
        compileOnly(project(":native:cli-native"))
        runtimeOnly(project(":kotlin-native:prepare:kotlin-native-compiler-embeddable"))
    } else {
        // when K/N is not available, depend on the required for compilation CLI part from bootstrap K/N distribution
        val bootstrapNativeCompiler = files(registerNativeBootstrapDistribution().map { it.compilerClasspath })
        val archiveOperations = serviceOf<ArchiveOperations>()
        val bootstrapK2NativeStub = tasks.register<Jar>("bootstrapKotlinNativeCompilerStub") {
            description = "Extracts the Kotlin/Native CLI entry point classes from the bootstrap Kotlin/Native compiler for compilation"
            archiveClassifier.set("bootstrap-k2native-stub")
            destinationDirectory.set(layout.buildDirectory.dir("bootstrapKotlinNative"))
            from(bootstrapNativeCompiler.elements.map { jars -> jars.map { archiveOperations.zipTree(it) } }) {
                include("org/jetbrains/kotlin/cli/bc/**")
            }
        }
        compileOnly(files(bootstrapK2NativeStub))
    }

    testCompileOnly(project(":compiler:cli"))
    testCompileOnly(intellijPlatformUtil())
    testImplementation(project(":compiler:incremental-compilation-impl"))
    testImplementation(project(":native:kotlin-native-utils"))
    testImplementation(project(":daemon-common"))
    testImplementation(project(":kotlin-daemon-client"))
    testImplementation(kotlinTest("junit5"))
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
}

projectTests {
    testTask {
        useJUnitPlatform()
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
    taskName = "generateBtaSources",
    generatorProject = ":compiler:build-tools:kotlin-build-tools-generator",
    generatorMainClass = "org.jetbrains.kotlin.buildtools.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            version.toString(),
            "impl",
            "jvmCompilerArguments,wasmArguments,jsArguments,metadataArguments",
        )
    },
)
