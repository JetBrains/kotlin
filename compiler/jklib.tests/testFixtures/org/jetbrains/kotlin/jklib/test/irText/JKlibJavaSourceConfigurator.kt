/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class JKlibJavaSourceConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(JvmEnvironmentConfigurationDirectives)

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val registeredDirectives = module.directives
        val jdkKind = JvmEnvironmentConfigurator.extractJdkKind(registeredDirectives)
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

        val withReflect = JvmEnvironmentConfigurationDirectives.WITH_REFLECT in module.directives

        if (withReflect) {
            testServices.assertions.fail { "WITH_REFLECT is not supported in JKlib tests" }
        }

        configuration.configureJdkClasspathRoots()

        val javaFiles = module.files.filter { it.name.endsWith(".java") }
        if (javaFiles.isEmpty()) return

        javaFiles.forEach { testServices.sourceFileProvider.getOrCreateRealFileForSourceFile(it) }

        val javaDir = testServices.sourceFileProvider.getJavaSourceDirectoryForModule(module)
        val jvmClasspathRoots = configuration.jvmClasspathRoots.map { it.absolutePath }

        try {
            val compiledJar = MockLibraryUtil.compileJavaFilesLibraryToJar(
                javaDir.path,
                "${module.name}-java-binaries",
                extraClasspath = jvmClasspathRoots,
                assertions = testServices.assertions,
                useJava11 = true // Requires jdk.11.home to be set in build.gradle.kts
            )
            configuration.addJvmClasspathRoot(compiledJar)
        } catch (e: Throwable) {
            testServices.assertions.fail {
                "Java Compilation failed. Notice: This might be caused by a circular dependency between Java and Kotlin sources, which is not supported in JKlib since it compiles Java to a JAR independently.\n" +
                        "Underlying error: ${e.message}"
            }
        }
    }
}
