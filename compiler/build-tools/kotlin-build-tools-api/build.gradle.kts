import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("project-tests-convention")
}

configureKotlinCompileTasksGradleCompatibility()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
    compileOnly(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
    embedded(project(":compiler:build-tools:kotlin-build-tools-jdk-utils"))
    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
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
    testTask(jUnitMode = JUnitMode.JUnit5)
}

generatedSourcesTask(
    taskName = "generateBtaArguments",
    generatorProject = ":compiler:build-tools:kotlin-build-tools-options-generator",
    generatorRoot = "compiler/build-tools/kotlin-build-tools-options-generator/src",
    generatorMainClass = "org.jetbrains.kotlin.buildtools.options.generator.MainKt",
    argsProvider = { generationRoot ->
        listOf(
            generationRoot.toString(),
            version.toString(),
            "api",
            "jvmCompilerArguments",
        )
    }
)

// Everything below is copied from "generated-sources" plugin, as applying it in this particular module creates a dependency cycle and
// breaks the build. This should be deleted when a solution for the cyclical dependency problem is solved.
// See KT-79146

/**
 * This utility function creates [taskName] task, which invokes specified code generator which produces some new
 *   sources for the current module in the directory ./gen
 *
 * @param [taskName] name for the created task
 * @param [generatorProject] module of the code generator
 * @param [generatorRoot] path to the `src` directory of the code generator
 * @param [generatorMainClass] FQN of the generator main class
 * @param [argsProvider] used for specifying the CLI arguments to the generator.
 *   By default, it passes the pass to the generated sources (`./gen`)
 * @param [dependOnTaskOutput] set to false disable the gradle dependency between the generation task and the compilation of the current
 *   module. This is needed for cases when the module with generator depends on the module for which it generates new sources.
 *   Use it with caution
 */
fun Project.generatedSourcesTask(
    taskName: String,
    generatorProject: String,
    generatorRoot: String,
    generatorMainClass: String,
    argsProvider: JavaExec.(generationRoot: Directory) -> List<String> = { listOf(it.toString()) },
    dependOnTaskOutput: Boolean = true
): TaskProvider<JavaExec> {
    val generatorClasspath: Configuration by configurations.creating

    dependencies {
        generatorClasspath(project(generatorProject))
    }

    return generatedSourcesTask(
        taskName,
        generatorClasspath,
        generatorRoot,
        generatorMainClass,
        argsProvider,
        dependOnTaskOutput = dependOnTaskOutput,
    )
}

/**
 * The utility can be used for sources generation by third-party tools.
 * For instance, it's used for Kotlin and KDoc lexer generations by JFlex.
 */
fun Project.generatedSourcesTask(
    taskName: String,
    generatorClasspath: Configuration,
    generatorRoot: String,
    generatorMainClass: String,
    argsProvider: JavaExec.(generationRoot: Directory) -> List<String> = { listOf(it.toString()) },
    dependOnTaskOutput: Boolean = true,
    commonSourceSet: Boolean = false,
): TaskProvider<JavaExec> {
    val genPath = if (commonSourceSet) {
        "common/src/gen"
    } else {
        "gen"
    }
    val generationRoot = layout.projectDirectory.dir(genPath)
    val task = tasks.register<JavaExec>(taskName) {
        workingDir = rootDir
        classpath = generatorClasspath
        mainClass.set(generatorMainClass)
        systemProperties["line.separator"] = "\n"
        args(argsProvider(generationRoot))

        @Suppress("NAME_SHADOWING")
        val generatorRoot = "$rootDir/$generatorRoot"
        val generatorConfigurationFiles = fileTree(generatorRoot) {
            include("**/*.kt")
        }

        inputs.files(generatorConfigurationFiles)
        outputs.dir(generationRoot)
    }

    if (!commonSourceSet) {
        sourceSets.named("main") {
            val dependency: Any = when (dependOnTaskOutput) {
                true -> task
                false -> generationRoot
            }
            java.srcDirs(dependency)
        }
    }

    apply(plugin = "idea")
    (this as ExtensionAware).extensions.configure<IdeaModel>("idea") {
        this.module.generatedSourceDirs.add(generationRoot.asFile)
    }
    return task
}

private val Project.sourceSets: SourceSetContainer
    get() = extensions.getByType<JavaPluginExtension>().sourceSets


