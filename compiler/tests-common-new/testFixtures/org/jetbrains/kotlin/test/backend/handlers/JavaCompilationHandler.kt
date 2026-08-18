/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmModularRoots
import org.jetbrains.kotlin.codegen.CodegenTestUtil
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.JavaCompilationResult
import org.jetbrains.kotlin.test.compileJavaFiles
import org.jetbrains.kotlin.test.java.JavaCompilerFacade
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.getRealJavaFiles
import org.jetbrains.kotlin.test.services.isModuleInfoJavaFile
import org.jetbrains.kotlin.test.services.javaFiles
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.services.sourceFileProvider
import org.jetbrains.kotlin.test.utils.withExtension

/**
 * Compiles the `.java` files of a module with real javac against the just-compiled Kotlin classes.
 *
 * If a sibling `<testName>.javaerr.txt` golden file exists, javac is expected to fail and its diagnostics
 * are compared against that file. Otherwise, javac is expected to succeed.
 */
class JavaCompilationHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        checkArtifact(info)
        if (module.javaFiles.isEmpty()) return

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val outputDir = testServices.compiledClassesManager.compileKotlinToDiskAndGetOutputDir(module, info.classFileFactory)
        val javaClasspath = listOf(outputDir.path) +
                configuration.jvmClasspathRoots.map { it.absolutePath } +
                configuration.jvmModularRoots.map { it.absolutePath }

        val javacOptions = JavaCompilerFacade.extractJavacOptions(
            module,
            configuration[JVMConfigurationKeys.JVM_TARGET],
            configuration.getBoolean(JVMConfigurationKeys.ENABLE_JVM_PREVIEW)
        )
        val finalJavacOptions = CodegenTestUtil.prepareJavacOptions(
            javaClasspath, javacOptions, outputDir,
            /* isJava9Module = */ module.files.any { it.isModuleInfoJavaFile }
        )

        val javaFiles = testServices.sourceFileProvider.getRealJavaFiles(module)
        val result = compileJavaFiles(javaFiles, finalJavacOptions, JavaCompilerFacade.getExplicitJdkHome(module))

        val javaErrFile = testServices.moduleStructure.originalTestDataFiles.first().withExtension("javaerr.txt")
        if (javaErrFile.exists()) {
            val diagnostics = if (result is JavaCompilationResult.Failure) result.diagnostics else ""
            assertions.assertEqualsToFile(javaErrFile, diagnostics)
        } else {
            result.assertSuccessful()
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}
}
