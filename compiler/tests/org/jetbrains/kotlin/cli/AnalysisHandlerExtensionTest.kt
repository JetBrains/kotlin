/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.cli

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.reflect.KClass
import kotlin.test.assertEquals

private data class TestKtFile(
    val name: String,
    val content: String
)

private val simple = TestKtFile("Simple.kt", "class Simple")
private val classNotFound = TestKtFile("C.kt", "class C : ClassNotFound")
private val repeatedAnalysis = TestKtFile("D.kt", "class D : Generated")

class AnalysisHandlerExtensionTest : TestCaseWithTmpdir() {

    // Writes the service file only; CustomComponentRegistrar is already in classpath.
    private fun writePlugin(klass: KClass<out ComponentRegistrar>): String {
        val jarFile = tmpdir.resolve("plugin.jar")
        ZipOutputStream(jarFile.outputStream()).use {
            val entry = ZipEntry("META-INF/services/org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar")
            it.putNextEntry(entry)
            it.write(klass.java.name.toByteArray())
        }
        return jarFile.absolutePath
    }

    private fun runTest(
        compiler: CLITool<*>,
        src: TestKtFile,
        klass: KClass<out ComponentRegistrar>,
        expectedExitCode: ExitCode = ExitCode.OK,
        extras: List<String> = emptyList(),
        messageRenderer: MessageRenderer? = null,
    ) {
        val mainKt = tmpdir.resolve(src.name).apply {
            writeText(src.content)
        }
        val plugin = writePlugin(klass)
        val args = listOf("-Xplugin=$plugin", mainKt.absolutePath)
        val outputPath = listOf(
            "-language-version", "1.9",
            "-d", tmpdir.resolve("out").absolutePath
        )

        val (output, exitCode) = CompilerTestUtil.executeCompiler(compiler, args + outputPath + extras, messageRenderer)
        assertEquals(expectedExitCode, exitCode, output)
    }

    fun testShouldNotGenerateCodeJVM() {
        runTest(K2JVMCompiler(), classNotFound, CustomComponentRegistrar::class)
    }

    fun testShouldNotGenerateCodeMetadata() {
        runTest(K2MetadataCompiler(), classNotFound, CustomComponentRegistrar::class)
    }

    fun testRepeatedAnalysisJVM() {
        runTest(K2JVMCompiler(), repeatedAnalysis, CustomComponentRegistrar::class)
    }

    fun testRepeatedAnalysisMetadata() {
        runTest(K2MetadataCompiler(), repeatedAnalysis, CustomComponentRegistrar::class)
    }

    fun testAnalysisError() {
        runTest(K2JVMCompiler(), simple, ErrorAnalysisComponentRegistrar::class, ExitCode.COMPILATION_ERROR)
    }

    fun testAnalysisErrorWithUnresolvedOptIn() {
        // KT-30172
        runTest(
            K2JVMCompiler(), simple, ErrorAnalysisComponentRegistrar::class, ExitCode.COMPILATION_ERROR,
            listOf("-Xopt-in=Unresolved")
        )
    }
}

class CustomComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        AnalysisHandlerExtension.registerExtension(project, CustomAnalysisHandler())
    }
}

// Assume that the directory of input files is writeable!
private class CustomAnalysisHandler : AnalysisHandlerExtension {
    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        val filenames = files.map { it.name }
        if (repeatedAnalysis.name in filenames) {
            if ("Generated.kt" in filenames) {
                return null
            } else {
                val outDir = File(files.single { it.name == repeatedAnalysis.name }.virtualFilePath).parentFile
                outDir.resolve("Generated.kt").apply {
                    writeText("interface Generated")
                }
                return AnalysisResult.RetryWithAdditionalRoots(BindingContext.EMPTY, module, emptyList(), listOf(outDir))
            }
        }
        return AnalysisResult.success(BindingContext.EMPTY, module, false)
    }
}

class ErrorAnalysisComponentRegistrar : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        AnalysisHandlerExtension.registerExtension(project, ErrorAnalysisHandlerExtension())
    }
}

private class ErrorAnalysisHandlerExtension : AnalysisHandlerExtension {
    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult = AnalysisResult.compilationError(BindingContext.EMPTY)
}
