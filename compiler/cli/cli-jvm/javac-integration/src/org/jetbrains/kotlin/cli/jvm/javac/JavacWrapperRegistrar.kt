/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.javac

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.mock.MockProject
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.javaSourceRoots
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.javac.JavacWrapper
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

object JavacWrapperRegistrar {
    private const val JAVAC_CONTEXT_CLASS = "com.sun.tools.javac.util.Context"

    fun registerJavac(
        project: MockProject,
        configuration: CompilerConfiguration,
        javaFiles: List<File>,
        kotlinFiles: List<KtFile>,
        arguments: Array<String>?,
        bootClasspath: List<File>?,
        sourcePath: List<File>?,
        lightClassGenerationSupport: LightClassGenerationSupport,
        packagePartsProviders: List<JvmPackagePartProvider>
    ): Boolean {
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        try {
            Class.forName(JAVAC_CONTEXT_CLASS)
        } catch (_: ClassNotFoundException) {
            messageCollector.report(ERROR, "'$JAVAC_CONTEXT_CLASS' class can't be found ('tools.jar' is not found)")
            return false
        }

        val context = Context()
        JavacLogger.preRegister(context, messageCollector)

        val jvmClasspathRoots = configuration.jvmClasspathRoots
        val outputDirectory = configuration.get(JVMConfigurationKeys.OUTPUT_DIRECTORY)
        val compileJava = configuration.getBoolean(JVMConfigurationKeys.COMPILE_JAVA)
        val kotlinSupertypesResolver = JavacWrapperKotlinResolverImpl(lightClassGenerationSupport)

        val javacWrapper = JavacWrapper(
            javaFiles, kotlinFiles, arguments, jvmClasspathRoots, bootClasspath, sourcePath,
            kotlinSupertypesResolver, packagePartsProviders, compileJava, outputDirectory, context
        )

        project.registerService(JavacWrapper::class.java, javacWrapper)

        return true
    }
}

private val KotlinCoreEnvironment.allJavaFiles: List<File>
    get() = configuration.javaSourceRoots
        .mapNotNull(this::findLocalFile)
        .flatMap { it.javaFiles }
        .map { File(it.canonicalPath!!) }

private val VirtualFile.javaFiles: List<VirtualFile>
    get() = mutableListOf<VirtualFile>().apply {
        VfsUtilCore.processFilesRecursively(this@javaFiles) { file ->
            if (file.extension == JavaFileType.DEFAULT_EXTENSION || file.fileType == JavaFileType.INSTANCE) {
                add(file)
            }
            true
        }
    }

fun KotlinCoreEnvironment.registerJavac(
    javaFiles: List<File> = allJavaFiles,
    kotlinFiles: List<KtFile> = getSourceFiles(),
    arguments: Array<String>? = null,
    bootClasspath: List<File>? = null,
    sourcePath: List<File>? = null
): Boolean {
    return JavacWrapperRegistrar.registerJavac(
        projectEnvironment.project, configuration, javaFiles, kotlinFiles, arguments, bootClasspath, sourcePath,
        LightClassGenerationSupport.getInstance(project), packagePartProviders
    )
}
