import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val flexGeneratorDependencies = configurations.dependencyScope("flexGeneratorDependencies")
val flexGeneratorClasspath = configurations.resolvable("flexGeneratorClasspath") {
    extendsFrom(flexGeneratorDependencies)
}

dependencies {
    compileOnly(intellijCore())
    compileOnly(libs.org.jetbrains.annotations)

    implementation(project(":compiler:psi:psi-api"))

    flexGeneratorDependencies.name(commonDependency("org.jetbrains.intellij.deps.jflex", "jflex")) {
        // Flex brings many unrelated dependencies, so we are dropping them because only a flex `.jar` file is needed.
        // It can be probably removed when https://github.com/JetBrains/intellij-deps-jflex/issues/10 is fixed.
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

fun registerLexerGeneration(
    taskName: String,
    lexerDir: String,
    flexFileName: String,
    generatedFileName: String,
    configure: JavaExec.(generatedFile: RegularFile) -> Unit = {},
): TaskProvider<JavaExec> {
    val flexFile = "$lexerDir/$flexFileName"
    val generatedFile = layout.projectDirectory.file("$lexerDir/$generatedFileName")
    // skeleton source: https://github.com/JetBrains/intellij-community/blob/486d14622bd240ceccd844a0e65d7c69e7e44735/tools/lexer/idea-flex.skeleton
    // TODO: KT-77207 (Use JFlex embedded skeleton)
    val skeletonFile = layout.projectDirectory.file("idea-flex.skeleton")

    val task = tasks.register<JavaExec>(taskName) {
        classpath(flexGeneratorClasspath)
        mainClass.set("jflex.Main")
        maxHeapSize = "512m"
        // Unicode classes like `[:jletter:]` are computed from the JDK running JFlex, so its version affects the generated lexer
        javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
        systemProperty("line.separator", "\n")
        // The flex file path is relative to keep the `// source: ...` header of the generated lexer stable
        workingDir = layout.projectDirectory.asFile

        inputs.file(flexFile)
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .normalizeLineEndings()
            .withPropertyName("flexFile")
        inputs.file(skeletonFile)
            .withPathSensitivity(PathSensitivity.RELATIVE)
            .normalizeLineEndings()
            .withPropertyName("skeletonFile")
        outputs.file(generatedFile)
            .withPropertyName("generatedFile")

        argumentProviders.add(CommandLineArgumentProvider {
            listOf(
                "--quiet",
                "-skel", skeletonFile.asFile.absolutePath,
                "-d", lexerDir,
                "--nobak", // Prevent generating backup `.java~` files
                flexFile,
            )
        })

        configure(generatedFile)
    }
    registerInAggregateGenerateSources(taskName)

    tasks.withType<JavaCompile>().configureEach { mustRunAfter(task) }
    tasks.withType<KotlinCompilationTask<*>>().configureEach { mustRunAfter(task) }

    return task
}

val generateKotlinLexer = registerLexerGeneration(
    taskName = "generateKotlinLexer",
    lexerDir = "src/org/jetbrains/kotlin/lexer",
    flexFileName = "Kotlin.flex",
    generatedFileName = "_JetLexer.java",
) { generatedFile ->
    doLast {
        // This is a hack, but we don't want to alter the skeleton we are using now, because being in sync with IDEA is more important
        val original = "throw new KotlinLexerException(message);"
        val replacement = """throw new KotlinLexerException(message + "\n at '" + yytext() + "'\n" + zzBuffer);"""
        val content = generatedFile.asFile.readText()
        check(original in content) { "Cannot find '$original' in $generatedFile" }
        generatedFile.asFile.writeText(content.replace(original, replacement))
    }
}

val generateKDocLexer = registerLexerGeneration(
    taskName = "generateKDocLexer",
    lexerDir = "src/org/jetbrains/kotlin/kdoc/lexer",
    flexFileName = "KDoc.flex",
    generatedFileName = "_KDocLexer.java",
)

tasks.register("lexer") {
    group = "Generate"
    description = "Generates Kotlin and KDoc lexers"
    dependsOn(generateKotlinLexer, generateKDocLexer)
}
