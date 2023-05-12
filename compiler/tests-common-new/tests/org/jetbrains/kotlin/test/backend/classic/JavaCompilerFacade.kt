/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.classic

import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.compileJavaFilesExternally
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.USE_JAVAC_BASED_ON_JVM_TARGET
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class JavaCompilerFacade(private val testServices: TestServices) {
    fun compileJavaFiles(module: TestModule, configuration: CompilerConfiguration, classFileFactory: ClassFileFactory) {
        if (module.javaFiles.isEmpty()) return
        val javaClasspath =
            listOf(testServices.compiledClassesManager.getCompiledKotlinDirForModule(module, classFileFactory).path) +
                    configuration.jvmClasspathRoots.map { it.absolutePath }

        val javaClassesOutputDirectory = testServices.compiledClassesManager.getOrCreateCompiledJavaDirForModule(module)

        val javacOptions = extractJavacOptions(
            module,
            configuration[JVMConfigurationKeys.JVM_TARGET],
            configuration.getBoolean(JVMConfigurationKeys.ENABLE_JVM_PREVIEW)
        )
        val finalJavacOptions = CodegenTestUtil.prepareJavacOptions(
            javaClasspath,
            javacOptions,
            javaClassesOutputDirectory
        )

        val javaFiles = testServices.sourceFileProvider.getRealJavaFiles(module)
        val ignoreErrors = CodegenTestDirectives.IGNORE_JAVA_ERRORS in module.directives
        compileJavaFiles(module, configuration[JVMConfigurationKeys.JVM_TARGET] ?: JvmTarget.DEFAULT, javaFiles, finalJavacOptions, ignoreErrors)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun extractJavacOptions(module: TestModule, kotlinTarget: JvmTarget?, isJvmPreviewEnabled: Boolean): List<String> {
        return buildList {
            addAll(module.directives[CodegenTestDirectives.JAVAC_OPTIONS])
            if (kotlinTarget != null && isJvmPreviewEnabled) {
                add("--release")
                add(kotlinTarget.description)
                add("--enable-preview")
                return@buildList
            }
            CodegenTestUtil.computeJavaTarget(this, kotlinTarget)?.let { javaTarget ->
                add("-source")
                add(javaTarget)
                add("-target")
                add(javaTarget)
            }
        }
    }

    private fun compileJavaFiles(module: TestModule, jvmTarget: JvmTarget, files: List<File>, javacOptions: List<String>, ignoreErrors: Boolean) {
        if (USE_JAVAC_BASED_ON_JVM_TARGET !in module.directives) {
            org.jetbrains.kotlin.test.compileJavaFiles(
                files,
                javacOptions,
                assertions = testServices.assertions,
                ignoreJavaErrors = ignoreErrors
            )
            return
        }
        val jdkHome = when (jvmTarget) {
            JvmTarget.JVM_1_8 -> KtTestUtil.getJdk8Home()
            JvmTarget.JVM_9,
            JvmTarget.JVM_11 -> KtTestUtil.getJdk11Home()
            JvmTarget.JVM_15,
            JvmTarget.JVM_17 -> KtTestUtil.getJdk17Home()
            else -> null
        } ?: error("JDK for $jvmTarget is not found")

        compileJavaFilesExternally(files, javacOptions, jdkHome)
    }
}
