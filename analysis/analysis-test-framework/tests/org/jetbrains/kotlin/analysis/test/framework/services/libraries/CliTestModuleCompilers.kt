/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.standardLibrariesPathProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.io.path.absolutePathString
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists
import kotlin.io.path.outputStream
import kotlin.io.path.pathString

abstract class CliTestModuleCompiler : TestModuleCompiler() {
    protected abstract fun buildPlatformCompilerOptions(module: TestModule, testServices: TestServices): List<String>

    protected abstract fun doCompile(
        sourcesPath: Path,
        options: List<String>,
        libraryOutputPath: Path,
        extraClasspath: List<String>,
    )

    protected abstract fun libraryOutputPath(inputPath: Path, libraryName: String): Path

    override fun compile(
        tmpDir: Path,
        module: TestModule,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
    ): Path {
        val compilationErrorExpected = Directives.COMPILATION_ERRORS in module.directives
        val library = try {
            val outputPath = libraryOutputPath(tmpDir, module.name)
            doCompile(
                tmpDir,
                buildCompilerOptions(module, testServices),
                outputPath,
                buildExtraClasspath(module, dependencyBinaryRoots, testServices)
            )
            outputPath
        } catch (e: Throwable) {
            if (!compilationErrorExpected) {
                throw IllegalStateException("Unexpected compilation error while compiling library", e)
            }
            null
        }
        if (library?.exists() == true && compilationErrorExpected) {
            error("Compilation error expected but, code was compiled successfully")
        }
        if (library == null || library.notExists()) {
            throw LibraryWasNotCompiledDueToExpectedCompilationError()
        }
        return library
    }

    override fun compileTestModuleToLibrarySources(module: TestModule, testServices: TestServices): Path {
        val tmpDir = KtTestUtil.tmpDir("testSourcesToCompile").toPath()
        val librarySourcesPath = tmpDir / "${module.name}-sources.jar"
        val manifest = Manifest().apply { mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0" }
        JarOutputStream(librarySourcesPath.outputStream(), manifest).use { jarOutputStream ->
            for (testFile in module.files) {
                val text = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
                addFileToJar(testFile.relativePath, text, jarOutputStream)
            }
        }

        return librarySourcesPath
    }

    private fun buildExtraClasspath(
        module: TestModule,
        dependencyBinaryRoots: Collection<Path>,
        testServices: TestServices,
    ): List<String> = buildList {
        addAll(buildPlatformExtraClasspath(module, testServices))
        dependencyBinaryRoots.mapTo(this) { it.pathString }
    }

    protected open fun buildPlatformExtraClasspath(module: TestModule, testServices: TestServices): List<String> = emptyList()

    private fun buildCompilerOptions(module: TestModule, testServices: TestServices): List<String> = buildList {
        addAll(buildCommonCompilerOptions(module))
        addAll(buildPlatformCompilerOptions(module, testServices))
    }

    private fun buildCommonCompilerOptions(module: TestModule): List<String> = buildList {
        module.directives.singleOrZeroValue(LanguageSettingsDirectives.API_VERSION)?.let { apiVersion ->
            addAll(listOf(CommonCompilerArguments::apiVersion.cliArgument, apiVersion.versionString))
        }

        module.directives.singleOrZeroValue(LanguageSettingsDirectives.LANGUAGE_VERSION)?.let { languageVersion ->
            addAll(listOf(CommonCompilerArguments::languageVersion.cliArgument, languageVersion.versionString))
        }

        module.directives.singleOrZeroValue(LanguageSettingsDirectives.LANGUAGE)?.let {
            add("-XXLanguage:$it")
        }

        if (LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in module.directives) {
            add(CommonCompilerArguments::allowKotlinPackage.cliArgument)
        }

        addAll(module.directives[Directives.COMPILER_ARGUMENTS])
    }

    private fun addFileToJar(path: String, text: String, jarOutputStream: JarOutputStream) {
        jarOutputStream.putNextEntry(JarEntry(path))
        ByteArrayInputStream(text.toByteArray()).copyTo(jarOutputStream)
        jarOutputStream.closeEntry()
    }
}

object JvmJarTestModuleCompiler : CliTestModuleCompiler() {
    override fun libraryOutputPath(inputPath: Path, libraryName: String): Path =
        inputPath / "$libraryName.jar"

    override fun buildPlatformCompilerOptions(module: TestModule, testServices: TestServices): List<String> = buildList {
        module.directives[JvmEnvironmentConfigurationDirectives.JVM_TARGET].firstOrNull()?.let { jvmTarget ->
            addAll(listOf(K2JVMCompilerArguments::jvmTarget.cliArgument, jvmTarget.description))

            val jdkHome = when {
                jvmTarget <= JvmTarget.JVM_1_8 -> KtTestUtil.getJdk8Home()
                jvmTarget <= JvmTarget.JVM_11 -> KtTestUtil.getJdk11Home()
                jvmTarget <= JvmTarget.JVM_17 -> KtTestUtil.getJdk17Home()
                jvmTarget <= JvmTarget.JVM_21 -> KtTestUtil.getJdk21Home()
                else -> error("JDK for $jvmTarget is not found")
            }

            addAll(listOf(K2JVMCompilerArguments::jdkHome.cliArgument, jdkHome.toString()))
        }
        add("-XXLanguage:-${LanguageFeature.SkipStandaloneScriptsInSourceRoots.name}")
    }

    override fun doCompile(
        sourcesPath: Path,
        options: List<String>,
        libraryOutputPath: Path,
        extraClasspath: List<String>,
    ) {
        MockLibraryUtil.compileLibraryToJar(
            sourcesPath = sourcesPath.absolutePathString(),
            contentDir = sourcesPath.toFile(),
            jarName = libraryOutputPath.nameWithoutExtension,
            extraOptions = buildList<String> {
                addAll(options)
            },
            assertions = JUnit5Assertions,
            useJava11 = true,
            extraClasspath = extraClasspath,
        )
    }

    override fun buildPlatformExtraClasspath(module: TestModule, testServices: TestServices): List<String> = buildList {
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        for (file in compilerConfiguration.jvmClasspathRoots) {
            add(file.absolutePath)
        }
    }
}

object JsKlibTestModuleCompiler : CliTestModuleCompiler() {
    override fun buildPlatformCompilerOptions(module: TestModule, testServices: TestServices): List<String> {
        return listOf(
            K2JSCompilerArguments::libraries.cliArgument, testServices.standardLibrariesPathProvider.fullJsStdlib().absolutePath,
        )
    }

    override fun doCompile(
        sourcesPath: Path,
        options: List<String>,
        libraryOutputPath: Path,
        extraClasspath: List<String>,
    ) {
        val sourceFiles = sourcesPath.toFile().walkBottomUp()

        val commands = buildList {
            add(K2JSCompilerArguments::moduleName.cliArgument); add(libraryOutputPath.nameWithoutExtension)
            add(K2JSCompilerArguments::outputDir.cliArgument); add(libraryOutputPath.parent.absolutePathString())
            add(K2JSCompilerArguments::irProduceKlibFile.cliArgument)
            sourceFiles.mapTo(this) { it.absolutePath }
            addAll(options)
        }
        MockLibraryUtil.runJsCompiler(commands)
    }

    override fun libraryOutputPath(inputPath: Path, libraryName: String): Path =
        inputPath / "$libraryName.klib"
}

object MetadataKlibDirTestModuleCompiler : CliTestModuleCompiler() {
    override fun buildPlatformCompilerOptions(
        module: TestModule,
        testServices: TestServices,
    ): List<String> {
        return emptyList()
    }

    override fun doCompile(
        sourcesPath: Path,
        options: List<String>,
        libraryOutputPath: Path,
        extraClasspath: List<String>,
    ) {
        val sourceFiles = sourcesPath.toFile().walkBottomUp()

        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2MetadataCompiler(), buildList {
                addAll(sourceFiles.mapTo(this) { it.absolutePath })
                add(K2MetadataCompilerArguments::destination.cliArgument); add(libraryOutputPath.absolutePathString())
                add(K2MetadataCompilerArguments::moduleName.cliArgument); add(libraryOutputPath.nameWithoutExtension)
                add(K2MetadataCompilerArguments::classpath.cliArgument)
                addAll(listOf(ForTestCompileRuntime.stdlibCommonForTests().absolutePath) + extraClasspath)
                addAll(options)
            }
        )
    }

    override fun libraryOutputPath(inputPath: Path, libraryName: String): Path =
        inputPath / libraryName
}

/**
 * [DispatchingTestModuleCompiler] chooses the appropriate compiler for a module based on its platform.
 * In case all tests in a suite should compile libraries for a single platform, one of the underlying [TestModuleCompiler]s
 * can be registered directly. Once new test compilers are introduced, they should be added to [DispatchingTestModuleCompiler].
 */
object DispatchingTestModuleCompiler : TestModuleCompiler() {
    override fun compile(tmpDir: Path, module: TestModule, dependencyBinaryRoots: Collection<Path>, testServices: TestServices): Path {
        return getCompiler(module).compileTestModuleToLibrary(module, dependencyBinaryRoots, testServices)
    }

    override fun compileTestModuleToLibrarySources(module: TestModule, testServices: TestServices): Path {
        return getCompiler(module).compileTestModuleToLibrarySources(module, testServices)
    }

    private fun getCompiler(module: TestModule): CliTestModuleCompiler {
        return when {
            module.targetPlatform.isJvm() -> JvmJarTestModuleCompiler
            module.targetPlatform.isJs() -> JsKlibTestModuleCompiler
            module.targetPlatform.isCommon() -> MetadataKlibDirTestModuleCompiler
            else -> error("DispatchingTestModuleCompiler doesn't support the platform: ${module.targetPlatform}")
        }
    }
}
