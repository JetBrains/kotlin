/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.stubs.jvm

import org.jetbrains.kotlin.analysis.stubs.AbstractCompiledStubsTest
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.CliTestModuleCompiler
import org.jetbrains.kotlin.analysis.test.framework.services.libraries.TestModuleCompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.nameWithoutExtension

abstract class AbstractCompiledJvmAbiStubsTest : AbstractCompiledStubsTest(JvmPlatforms.defaultJvmPlatform) {
    override fun configureTest(builder: TestConfigurationBuilder) {
        super.configureTest(builder)
        builder.useAdditionalService<TestModuleCompiler> { JvmAbiTestModuleCompiler }
    }
}

private object JvmAbiTestModuleCompiler : CliTestModuleCompiler() {
    override fun libraryOutputPath(inputPath: Path, libraryName: String): Path =
        inputPath.resolve("$libraryName-abi.jar")

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
        val pluginJar = ForTestCompileRuntime.getFileFromProperty("kotlin.jvm.abi.jar.path")
        MockLibraryUtil.compileLibraryToJar(
            sourcesPath = sourcesPath.absolutePathString(),
            contentDir = sourcesPath.toFile(),
            jarName = "${libraryOutputPath.nameWithoutExtension}-full",
            extraOptions = buildList {
                addAll(options)
                add("-Xplugin=${pluginJar.absolutePath}")
                add("-P")
                add("plugin:org.jetbrains.kotlin.jvm.abi:outputDir=${libraryOutputPath.absolutePathString()}")
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
