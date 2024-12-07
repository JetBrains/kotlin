/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.java

import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.compileJavaFiles
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class JavaCompilerFacade(private val testServices: TestServices) {
    fun compileJavaFiles(module: TestModule, configuration: CompilerConfiguration, classFileFactory: ClassFileFactory) {
        if (module.javaFiles.isEmpty()) return

        val outputDir = testServices.compiledClassesManager.compileKotlinToDiskAndGetOutputDir(module, classFileFactory)
        val javaClasspath =
            listOf(outputDir.path) + configuration.jvmClasspathRoots.map { it.absolutePath }

        val javacOptions = extractJavacOptions(
            module,
            configuration[JVMConfigurationKeys.JVM_TARGET],
            configuration.getBoolean(JVMConfigurationKeys.ENABLE_JVM_PREVIEW)
        )
        val finalJavacOptions = CodegenTestUtil.prepareJavacOptions(
            javaClasspath, javacOptions, outputDir,
            /* isJava9Module = */ module.files.any { it.isModuleInfoJavaFile }
        )

        val javaFiles = testServices.sourceFileProvider.getRealJavaFiles(module)
        compileJavaFiles(javaFiles, finalJavacOptions, getExplicitJdkHome(module)).assertSuccessful()
    }

    companion object {
        fun extractJavacOptions(module: TestModule, kotlinTarget: JvmTarget?, isJvmPreviewEnabled: Boolean): List<String> {
            return buildList {
                addAll(module.directives[CodegenTestDirectives.JAVAC_OPTIONS])
                if (kotlinTarget != null) {
                    if (isJvmPreviewEnabled) {
                        add("--release")
                        add(kotlinTarget.description)
                        add("--enable-preview")
                    } else {
                        add("-source")
                        add(kotlinTarget.description)
                        add("-target")
                        add(kotlinTarget.description)
                    }
                }
            }
        }

        fun getExplicitJdkHome(module: TestModule): File? {
            return when (val jdkKind = module.directives.singleOrZeroValue(JvmEnvironmentConfigurationDirectives.JDK_KIND)) {
                TestJdkKind.FULL_JDK -> KtTestUtil.getJdk8Home()
                TestJdkKind.FULL_JDK_11 -> KtTestUtil.getJdk11Home()
                TestJdkKind.FULL_JDK_17 -> KtTestUtil.getJdk17Home()
                TestJdkKind.FULL_JDK_21 -> KtTestUtil.getJdk21Home()
                null -> JvmEnvironmentConfigurator.getJdkHomeFromProperty { null }
                else -> error("JDK $jdkKind does not support compilation")
            }
        }
    }
}
