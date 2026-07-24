/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import kotlin.io.path.*

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
        sourcesTempDirectory: Path,
        commonSourcesTempDirectory: Path?,
        module: TestModule,
        libraryName: String,
        dependencyBinaryRoots: Collection<Path>,
        resourceFiles: List<TestFile>,
        testServices: TestServices,
    ): Path {
        val allowedLibraryPlatforms = module.directives[Directives.LIBRARY_PLATFORMS].map { it.targetPlatform }
        val compilationErrorExpected = Directives.COMPILATION_ERRORS in module.directives
                || (allowedLibraryPlatforms.isNotEmpty() && module.targetPlatform(testServices) !in allowedLibraryPlatforms)

        val library = try {
            val outputPath = libraryOutputPath(sourcesTempDirectory, libraryName)
            doCompile(
                sourcesTempDirectory,
                buildCompilerOptions(module, testServices, commonSourcesTempDirectory),
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
        embedResourceFiles(library, resourceFiles, testServices)
        return library
    }

    /**
     * Embeds [resourceFiles] (marked with [TestModuleCompiler.Directives.LIBRARY_RESOURCE]) into the compiled [library]. The default
     * implementation does not support resource files. Compilers that produce a suitable archive should override this function.
     */
    protected open fun embedResourceFiles(library: Path, resourceFiles: List<TestFile>, testServices: TestServices) {
        require(resourceFiles.isEmpty()) {
            "${this::class.simpleName} does not support `LIBRARY_RESOURCE` files: ${resourceFiles.map { it.name }}"
        }
    }

    override fun compileSources(files: List<TestFile>, module: TestModule, testServices: TestServices): Path {
        val tmpDir = KtTestUtil.tmpDir("testSourcesToCompile").toPath()
        val librarySourcesPath = tmpDir / "${module.name}-sources.jar"
        val manifest = Manifest().apply { mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0" }
        JarOutputStream(librarySourcesPath.outputStream(), manifest).use { jarOutputStream ->
            for (testFile in files) {
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

    private fun buildCompilerOptions(
        module: TestModule,
        testServices: TestServices,
        commonSourcesTempDirectory: Path?,
    ): List<String> = buildList {
        addAll(buildCommonCompilerOptions(module))
        addAll(buildPlatformCompilerOptions(module, testServices))
        addAll(buildCommonSourcesCompilerOptions(commonSourcesTempDirectory))
    }

    private fun buildCommonCompilerOptions(module: TestModule): List<String> = buildList {
        module.directives.singleOrZeroValue(LanguageSettingsDirectives.API_VERSION)?.let { apiVersion ->
            addAll(listOf(CommonCompilerArguments::apiVersion.cliArgument, apiVersion.versionString))
        }

        module.directives.singleOrZeroValue(LanguageSettingsDirectives.LANGUAGE_VERSION)?.let { languageVersion ->
            addAll(listOf(CommonCompilerArguments::languageVersion.cliArgument, languageVersion.versionString))
        }

        module.directives[LanguageSettingsDirectives.LANGUAGE].forEach {
            add("-XXLanguage:$it")
        }

        if (LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in module.directives) {
            add(CommonCompilerArguments::allowKotlinPackage.cliArgument)
        }

        addAll(module.directives[Directives.COMPILER_ARGUMENTS])
    }

    private fun buildCommonSourcesCompilerOptions(commonSourcesTempDirectory: Path?): List<String> {
        if (commonSourcesTempDirectory == null) {
            return emptyList()
        }

        val commonSourcesPathString = commonSourcesTempDirectory.absolutePathString()

        return listOf(
            "-Xcommon-sources=$commonSourcesPathString",
            commonSourcesPathString // Also add common sources directly, as a free parameter
        )
    }

    private fun addFileToJar(path: String, text: String, jarOutputStream: JarOutputStream) {
        jarOutputStream.putNextEntry(JarEntry(path))
        ByteArrayInputStream(text.toByteArray()).copyTo(jarOutputStream)
        jarOutputStream.closeEntry()
    }
}

object JvmJarTestModuleCompiler : CliTestModuleCompiler() {
    override fun embedResourceFiles(library: Path, resourceFiles: List<TestFile>, testServices: TestServices) {
        if (resourceFiles.isEmpty()) return

        // `JarOutputStream` cannot append to an existing archive, so the produced JAR is reopened as a zip file system instead.
        FileSystems.newFileSystem(URI.create("jar:${library.toUri()}"), emptyMap<String, Any>()).use { jarFileSystem ->
            for (testFile in resourceFiles) {
                val content = testServices.sourceFileProvider.getContentOfSourceFile(testFile)
                val target = jarFileSystem.getPath("/").resolve(testFile.relativePath)
                target.parent?.createDirectories()
                target.writeText(content)
            }
        }
    }

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

        if (LanguageSettingsDirectives.JVM_EXPOSE_BOXED in module.directives) {
            add(K2JVMCompilerArguments::jvmExposeBoxed.cliArgument)
        }
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
            extraOptions = buildList {
                addAll(options)
            },
            useJava11 = true,
            extraClasspath = extraClasspath,
        )
    }

    override fun buildPlatformExtraClasspath(module: TestModule, testServices: TestServices): List<String> = buildList {
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module, CompilationStage.FIRST)
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
            sourceFiles.mapTo(this) { it.absolutePath }
            addAll(options)
        }

        MockLibraryUtil.runJsCompiler(commands)
    }

    override fun libraryOutputPath(inputPath: Path, libraryName: String): Path =
        inputPath / "$libraryName.klib"
}

object MetadataKlibDirTestModuleCompiler : CliTestModuleCompiler() {
    override fun compile(
        sourcesTempDirectory: Path,
        commonSourcesTempDirectory: Path?,
        module: TestModule,
        libraryName: String,
        dependencyBinaryRoots: Collection<Path>,
        resourceFiles: List<TestFile>,
        testServices: TestServices
    ): Path {
        check(commonSourcesTempDirectory == null) { "Dependent common sources aren't empty for a common module" }
        return super.compile(sourcesTempDirectory, null, module, libraryName, dependencyBinaryRoots, resourceFiles, testServices)
    }

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

        val commands = buildList<String> {
            addAll(sourceFiles.mapTo(this) { it.absolutePath })
            add(K2MetadataCompilerArguments::destination.cliArgument); add(libraryOutputPath.absolutePathString())
            add(K2MetadataCompilerArguments::moduleName.cliArgument); add(libraryOutputPath.nameWithoutExtension)
            // JS and Wasm platforms is excluded to allow inheritance from functional types and initializers in external declarations
            add("${K2MetadataCompilerArguments::targetPlatform.cliArgument}=JVM,Native")
            add(K2MetadataCompilerArguments::classpath.cliArgument)
            addAll(listOf(ForTestCompileRuntime.stdlibCommonForTests().absolutePath) + extraClasspath)
            addAll(options)
        }

        MockLibraryUtil.runMetadataCompiler(commands)
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
    override fun compile(
        sourcesTempDirectory: Path,
        commonSourcesTempDirectory: Path?,
        module: TestModule,
        libraryName: String,
        dependencyBinaryRoots: Collection<Path>,
        resourceFiles: List<TestFile>,
        testServices: TestServices
    ): Path {
        return getCompiler(module, testServices).compile(
            sourcesTempDirectory,
            commonSourcesTempDirectory,
            module,
            libraryName,
            dependencyBinaryRoots,
            resourceFiles,
            testServices
        )
    }

    override fun compileSources(files: List<TestFile>, module: TestModule, testServices: TestServices): Path {
        return getCompiler(module, testServices).compileSources(module.files, module, testServices)
    }

    private fun getCompiler(module: TestModule, testServices: TestServices): CliTestModuleCompiler {
        val targetPlatform = module.targetPlatform(testServices)
        return when {
            targetPlatform.isJvm() -> JvmJarTestModuleCompiler
            targetPlatform.isJs() -> JsKlibTestModuleCompiler
            targetPlatform.isCommon() -> MetadataKlibDirTestModuleCompiler
            else -> error("DispatchingTestModuleCompiler doesn't support the platform: $targetPlatform")
        }
    }
}
