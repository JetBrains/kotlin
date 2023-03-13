/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.javac

import com.intellij.mock.MockProject
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.config.jvmClasspathRoots
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
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        try {
            Class.forName(JAVAC_CONTEXT_CLASS)
        } catch (e: ClassNotFoundException) {
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
