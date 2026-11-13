/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.codegen.forTestCompile.JavaForeignAnnotationType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.ForeignAnnotationsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.additionalClassPathForJavaCompilationOrAnalysis
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.javaFiles
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.services.temporaryDirectoryManager
import org.jetbrains.kotlin.test.services.transitiveDependsOnDependencies
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.util.CompiledLibraryCache
import java.io.File

class JKlibJavaSourceConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        private val libraryCache = CompiledLibraryCache()
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(JvmEnvironmentConfigurationDirectives, ForeignAnnotationsDirectives)

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val registeredDirectives = module.directives
        val jdkKind = JvmEnvironmentConfigurator.extractJdkKind(registeredDirectives, default = TestJdkKind.FULL_JDK_17)
        JvmEnvironmentConfigurator.getJdkHome(jdkKind)?.let { configuration.put(JVMConfigurationKeys.JDK_HOME, it) }

        when (jdkKind) {
            TestJdkKind.MOCK_JDK, TestJdkKind.MODIFIED_MOCK_JDK -> {
                configuration.put(JVMConfigurationKeys.NO_JDK, true)
                configuration.addJvmClasspathRoot(KtTestUtil.findMockJdkRtJar())
            }
            else -> {
                JvmEnvironmentConfigurator.getJdkClasspathRoot(jdkKind)?.let { configuration.addJvmClasspathRoot(it) }
            }
        }

        configuration.configureJdkClasspathRoots()

        // Like the Kotlin/JVM tests, analyze the Java sources together with the Kotlin ones instead of compiling them first, so
        // that Java sources can reference Kotlin declarations of the same module. `addSourcesForDependsOnClosure` compiles the
        // Kotlin sources of the whole `dependsOn` closure in this configuration, so its Java sources belong here too.
        val modulesWithJavaFiles = module.transitiveDependsOnDependencies(includeSelf = true, reverseOrder = true)
            .filter { it.javaFiles.isNotEmpty() }
        // KLIBs don't contain Java classes, so the Java sources of the dependencies are compiled with javac and passed as class
        // files, like the Kotlin/JVM tests do.
        val dependencyJavaJars = module.javaJarsOfDependencies()
        if (modulesWithJavaFiles.isEmpty() && dependencyJavaJars.isEmpty()) return

        for (moduleWithJavaFiles in modulesWithJavaFiles) {
            moduleWithJavaFiles.javaFiles.forEach { testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(it) }
            configuration.addJavaSourceRoot(testServices.sourceFileProvider.getJavaSourceDirectoryForModule(moduleWithJavaFiles))
        }
        configuration.addJvmClasspathRoots(dependencyJavaJars)

        configuration.addJvmClasspathRoots(module.javaClasspath())
    }

    private val javaJars = mutableMapOf<TestModule, File?>()

    private fun TestModule.javaJarsOfDependencies(): List<File> =
        (regularDependencies + friendDependencies).mapNotNull { it.dependencyModule.compileJavaFilesToJar() }

    private fun TestModule.compileJavaFilesToJar(): File? = javaJars.getOrPut(this) {
        if (javaFiles.isEmpty()) return@getOrPut null
        javaFiles.forEach { testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(it) }
        MockLibraryUtil.compileLibraryToJar(
            sourcesPath = testServices.sourceFileProvider.getJavaSourceDirectoryForModule(this).path,
            contentDir = testServices.temporaryDirectoryManager.getOrCreateTempDirectory("java-binaries-$name"),
            jarName = name,
            allowKotlinSources = false,
            extraClasspath = (javaJarsOfDependencies() + javaClasspath()).map { it.path },
        )
    }

    private fun TestModule.javaClasspath(): List<File> {
        val annotationsJar = if (ForeignAnnotationsDirectives.ENABLE_FOREIGN_ANNOTATIONS in directives) {
            libraryCache.getOrCompile("java8-annotations") {
                MockLibraryUtil.compileJavaFilesLibraryToJar(
                    JavaForeignAnnotationType.Java8Annotations.path,
                    "java8-annotations",
                    assertions = testServices.assertions,
                    extraOptions = listOf("-Xlint:-options")
                )
            }
        } else {
            KtTestUtil.getAnnotationsJar()
        }
        return listOf(annotationsJar) +
                testServices.additionalClassPathForJavaCompilationOrAnalysis?.classPath?.map(::File).orEmpty()
    }
}
