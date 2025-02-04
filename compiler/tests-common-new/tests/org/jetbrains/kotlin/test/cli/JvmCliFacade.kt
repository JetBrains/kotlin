/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.cli

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.cli.CliDirectives.FORCE_COMPILE_AS_JAVA_MODULE
import org.jetbrains.kotlin.test.cli.CliDirectives.KOTLINC_ARGS
import org.jetbrains.kotlin.test.compileJavaFiles
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JDK_KIND
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.JVM_TARGET
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.java.JavaCompilerFacade
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.model.DependencyKind.Binary
import org.jetbrains.kotlin.test.model.DependencyRelation.DependsOnDependency
import org.jetbrains.kotlin.test.model.DependencyRelation.FriendDependency
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled
import java.io.File

@RequiresOptIn(
    message = "This facade is deprecated and used only for K1 tests. For K2 tests use FirCliJvmFacade and related instead.",
    level = RequiresOptIn.Level.ERROR,
)
annotation class DeprecatedCliFacades

/**
 * Compiles Kotlin and Java sources in the module to .class files. Kotlin is compiled via CLI with `-Xrender-internal-diagnostic-names`,
 * which allows [CliMetaInfoHandler] to parse diagnostics and their locations from the compiler output, and place meta info markers.
 *
 * Note that compiler messages without locations (such as warnings about enabled language features or versions) are rendered normally
 * and dumped into `.out`/`.fir.out` files. This happens only if the compiler output is non-empty,
 * and [CliDirectives.CHECK_COMPILER_OUTPUT] is enabled.
 */
@DeprecatedCliFacades
class ClassicJvmCliFacade(private val testServices: TestServices) : AbstractTestFacade<ResultingArtifact.Source, CliArtifact>() {
    override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind

    override val outputKind: TestArtifactKind<CliArtifact>
        get() = CliArtifact.Kind

    override fun shouldTransform(module: TestModule): Boolean = true

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(CliDirectives)

    override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): CliArtifact {
        val outputDir = testServices.getOrCreateTempDirectory("module_${module.name}_classes")

        val (dependencies, friends) = module.computeDependencies()
        val kotlinResult = compileKotlin(module, outputDir, dependencies, friends)

        if (kotlinResult?.hasErrors != true) {
            compileJava(module, outputDir, dependencies)
        }

        return CliArtifact(outputDir, kotlinResult?.diagnostics.orEmpty(), kotlinResult?.otherOutput?.toString().orEmpty())
    }

    private data class ModuleDependencies(val dependencies: List<File>, val friends: List<File>)

    private fun TestModule.computeDependencies(): ModuleDependencies {
        val dependencies = allDependencies.map { (dependency, kind, relation) ->
            check(kind == Binary) { "Only binary dependencies are possible here: $name -> ${dependency.name}" }
            // TODO (KT-69158): add support for KMP in this facade.
            check(relation != DependsOnDependency) { "Only normal/friend relation is possible here: $name -> ${dependency.name}" }
            relation to testServices.artifactsProvider.getArtifact(dependency, CliArtifact.Kind).outputDir
        }
        return ModuleDependencies(
            dependencies.map { it.second },
            dependencies.filter { it.first == FriendDependency }.map { it.second },
        )
    }

    private fun compileKotlin(
        module: TestModule, outputDir: File, dependencies: List<File>, friends: List<File>,
    ): DiagnosticCollectingMessageRenderer? {
        val ktFiles = module.files.filter(TestFile::isKtFile)
        if (ktFiles.isEmpty()) return null

        val args = buildList {
            ktFiles.mapTo(this) { it.realFile().path }
            testServices.sourceFileProvider.getRealJavaFiles(module).mapTo(this) { it.path }

            if (JDK_KIND in module.directives) {
                add(K2JVMCompilerArguments::jdkHome.cliArgument)
                add(JavaCompilerFacade.getExplicitJdkHome(module)!!.absolutePath)
            }

            // Use '-no-stdlib' and pass the path to stdlib explicitly, because otherwise CLI compiler will try to find it in Kotlin home,
            // which might be empty because this module does not depend on `:dist`.
            val allDependencies = (dependencies + ForTestCompileRuntime.runtimeJarForTests())
                .joinToString(separator = File.pathSeparator, transform = File::getPath)
            add(K2JVMCompilerArguments::noStdlib.cliArgument)
            if (FORCE_COMPILE_AS_JAVA_MODULE in module.directives || module.files.any { it.isModuleInfoJavaFile }) {
                add(K2JVMCompilerArguments::javaModulePath.cliArgument(allDependencies))
            } else {
                add(K2JVMCompilerArguments::classpath.cliArgument)
                add(allDependencies)
            }

            if (friends.isNotEmpty()) {
                add(
                    K2JVMCompilerArguments::friendPaths.cliArgument(
                        friends.joinToString(separator = File.pathSeparator, transform = File::getPath)
                    )
                )
            }

            val jvmTarget = module.directives.singleOrZeroValue(JVM_TARGET)
            if (jvmTarget != null) {
                add(K2JVMCompilerArguments::jvmTarget.cliArgument)
                add(jvmTarget.description)
            }

            add(K2JVMCompilerArguments::destination.cliArgument)
            add(outputDir.path)

            add(K2JVMCompilerArguments::renderInternalDiagnosticNames.cliArgument)

            addAll(module.directives[KOTLINC_ARGS])

            add(CommonCompilerArguments::languageVersion.cliArgument)
            add(LanguageVersion.KOTLIN_1_9.versionString)
        }
        val diagnosticCollector = DiagnosticCollectingMessageRenderer()
        CompilerTestUtil.executeCompiler(K2JVMCompiler(), args, diagnosticCollector)
        return diagnosticCollector
    }

    private fun compileJava(module: TestModule, outputDir: File, dependencies: List<File>) {
        val javaFiles = module.files.filter(TestFile::isJavaFile)
        if (javaFiles.isNotEmpty()) {
            val javaClasspath = listOf(outputDir.path) + dependencies.map(File::getPath)
            val javacOptions = JavaCompilerFacade.extractJavacOptions(
                module,
                module.directives.singleOrZeroValue(JVM_TARGET),
                isJvmPreviewEnabled = false,
            )
            val finalJavacOptions = CodegenTestUtil.prepareJavacOptions(
                javaClasspath, javacOptions, outputDir,
                /* isJava9Module = */ module.files.any { it.isModuleInfoJavaFile }
            )
            val realFiles = testServices.sourceFileProvider.getRealJavaFiles(module)
            compileJavaFiles(realFiles, finalJavacOptions, JavaCompilerFacade.getExplicitJdkHome(module)).assertSuccessful()
        }
    }

    private fun TestFile.realFile(): File =
        testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(this)
}

@DeprecatedCliFacades
data class CliArtifact(
    val outputDir: File,
    val kotlinDiagnostics: List<CliDiagnostic>,
    val kotlinOutput: String,
) : ResultingArtifact.Binary<CliArtifact>() {
    object Kind : ArtifactKind<CliArtifact>("CliArtifact")

    override val kind: ArtifactKind<CliArtifact> get() = Kind
}

@DeprecatedCliFacades
abstract class CliArtifactHandler(
    testServices: TestServices,
    failureDisablesNextSteps: Boolean = false,
) : BinaryArtifactHandler<CliArtifact>(
    testServices,
    CliArtifact.Kind,
    failureDisablesNextSteps,
    doNotRunIfThereWerePreviousFailures = true,
)

@DeprecatedCliFacades
data class CliDiagnostic(
    val name: String,
    val lineBegin: Int,
    val lineEnd: Int,
    val columnBegin: Int,
    val columnEnd: Int,
)

@DeprecatedCliFacades
private class DiagnosticCollectingMessageRenderer : MessageRenderer {
    private val delegate = MessageRenderer.PLAIN_RELATIVE_PATHS

    val diagnostics = mutableListOf<CliDiagnostic>()
    val otherOutput = StringBuilder()
    var hasErrors = false

    override fun render(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ): String {
        if (severity.isError) hasErrors = true

        if (location != null) {
            val matchResult = diagnosticNameRegex.matchAt(message, 0)
            if (matchResult != null) {
                val (diagnosticName) = matchResult.destructured
                diagnostics.add(CliDiagnostic(diagnosticName, location.line, location.lineEnd, location.column, location.columnEnd))
                return ""
            }
        }

        otherOutput.appendLine(delegate.render(severity, message, location))
        return ""
    }

    override fun renderPreamble(): String = ""
    override fun renderUsage(usage: String): String = shouldNotBeCalled()
    override fun renderConclusion(): String = ""
    override fun getName(): String = this::class.java.simpleName

    companion object {
        private val diagnosticNameRegex = "\\[([A-Z_0-9]+)\\]".toRegex()
    }
}
