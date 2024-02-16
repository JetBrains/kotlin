/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services.libraries

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestModule
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
import kotlin.io.path.div
import kotlin.io.path.outputStream

abstract class CliTestModuleCompiler : TestModuleCompiler() {
    internal abstract val compilerKind: CompilerExecutor.CompilerKind

    protected abstract fun buildPlatformCompilerOptions(module: TestModule, testServices: TestServices): List<String>

    override fun compile(tmpDir: Path, module: TestModule, testServices: TestServices): Path = CompilerExecutor.compileLibrary(
        compilerKind,
        tmpDir,
        buildCompilerOptions(module, testServices),
        compilationErrorExpected = Directives.COMPILATION_ERRORS in module.directives,
        libraryName = module.name,
        extraClasspath = buildExtraClasspath(module, testServices),
    )

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

    private fun buildExtraClasspath(module: TestModule, testServices: TestServices): List<String> = buildList {
        addAll(buildPlatformExtraClasspath(module, testServices))
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

class JvmJarTestModuleCompiler : CliTestModuleCompiler() {
    override val compilerKind = CompilerExecutor.CompilerKind.JVM

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
    }

    override fun buildPlatformExtraClasspath(module: TestModule, testServices: TestServices): List<String> = buildList {
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        for (file in compilerConfiguration.jvmClasspathRoots) {
            add(file.absolutePath)
        }
    }
}

class JsKlibTestModuleCompiler : CliTestModuleCompiler() {
    override val compilerKind = CompilerExecutor.CompilerKind.JS

    override fun buildPlatformCompilerOptions(module: TestModule, testServices: TestServices): List<String> {
        return listOf(
            K2JSCompilerArguments::libraries.cliArgument, testServices.standardLibrariesPathProvider.fullJsStdlib().absolutePath,
        )
    }
}

/**
 * [DispatchingTestModuleCompiler] chooses the appropriate compiler for a module based on its platform.
 * In case all tests in a suite should compile libraries for a single platform, one of the underlying [TestModuleCompiler]s
 * can be registered directly. Once new test compilers are introduced, they should be added to [DispatchingTestModuleCompiler].
 */
class DispatchingTestModuleCompiler : TestModuleCompiler() {
    private val compilersByKind = mapOf(
        CompilerExecutor.CompilerKind.JVM to JvmJarTestModuleCompiler(),
        CompilerExecutor.CompilerKind.JS to JsKlibTestModuleCompiler(),
    )

    override fun compile(tmpDir: Path, module: TestModule, testServices: TestServices): Path {
        return getCompiler(module).compileTestModuleToLibrary(module, testServices)
    }

    override fun compileTestModuleToLibrarySources(module: TestModule, testServices: TestServices): Path {
        return getCompiler(module).compileTestModuleToLibrarySources(module, testServices)
    }

    private fun getCompiler(module: TestModule): CliTestModuleCompiler {
        val compilerKindForModule = when {
            module.targetPlatform.isJvm() -> CompilerExecutor.CompilerKind.JVM
            module.targetPlatform.isJs() -> CompilerExecutor.CompilerKind.JS
            else -> error("DispatchingTestModuleCompiler doesn't support the platform: ${module.targetPlatform}")
        }

        return compilersByKind[compilerKindForModule]
            ?: error("TestModuleCompiler is not available for ${compilerKindForModule.name}")
    }
}
