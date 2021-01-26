/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.classic

import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassFileFactory
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.compileJavaFilesExternally
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.javaFiles
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class JavaCompilerFacade(private val testServices: TestServices) {
    @OptIn(ExperimentalStdlibApi::class)
    fun compileJavaFiles(module: TestModule, configuration: CompilerConfiguration, classFileFactory: ClassFileFactory) {
        if (module.javaFiles.isEmpty()) return
        val javaClasspath = buildList {
            add(testServices.compiledClassesManager.getCompiledKotlinDirForModule(module, classFileFactory).path)
            addAll(configuration.jvmClasspathRoots.map { it.absolutePath })
            if (JvmEnvironmentConfigurationDirectives.ANDROID_ANNOTATIONS in module.directives) {
                add(ForTestCompileRuntime.androidAnnotationsForTests().path)
            }
        }

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

        val javaFiles = module.javaFiles.map { testServices.sourceFileProvider.getRealFileForSourceFile(it) }
        val ignoreErrors = CodegenTestDirectives.IGNORE_JAVA_ERRORS in module.directives
        compileJavaFiles(configuration[JVMConfigurationKeys.JVM_TARGET] ?: JvmTarget.DEFAULT, javaFiles, finalJavacOptions, ignoreErrors)
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

    private fun compileJavaFiles(jvmTarget: JvmTarget, files: List<File>, javacOptions: List<String>, ignoreErrors: Boolean) {
        val targetIsJava8OrLower = System.getProperty("java.version").startsWith("1.")
        if (targetIsJava8OrLower) {
            org.jetbrains.kotlin.test.compileJavaFiles(
                files,
                javacOptions,
                assertions = testServices.assertions,
                ignoreJavaErrors = ignoreErrors
            )
            return
        }
        val jdkHome = when (jvmTarget) {
            JvmTarget.JVM_9 -> KtTestUtil.getJdk9Home()
            JvmTarget.JVM_11 -> KtTestUtil.getJdk11Home()
            JvmTarget.JVM_15 -> KtTestUtil.getJdk15Home()
            else -> null
        } ?: error("JDK for $jvmTarget is not found")

        compileJavaFilesExternally(files, javacOptions, jdkHome)
    }
}
