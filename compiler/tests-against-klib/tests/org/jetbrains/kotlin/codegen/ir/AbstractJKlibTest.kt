/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.ObsoleteTestInfrastructure
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.arguments.K2JKlibCompilerArguments
import org.jetbrains.kotlin.cli.common.computeKotlinPaths
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler
import org.jetbrains.kotlin.codegen.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.nio.file.Paths

@OptIn(ObsoleteTestInfrastructure::class)
abstract class AbstractJKlibTest : AbstractBlackBoxCodegenTest() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        compileToKlibAndDeserializeIr(
            wholeFile.nameWithoutExtension,
            KtTestUtil.tmpDir("java-files").toString(),
            files
        )
    }

    // We need real (as opposed to virtual) files in order to produce a Klib.
    private fun loadMultiFilesReal(outputDir: String, files: List<TestFile>): List<String> {
        return files.map { testFile ->
            assert(testFile.name.endsWith(".kt"))
            val ktFile = File(Paths.get(outputDir, testFile.name).toString())
            ktFile.writeText(testFile.content)
            ktFile.toString()
        }
    }

    private fun compileToKlibAndDeserializeIr(klibName: String, outputDir: String, files: List<TestFile>) {
        val sourceFiles = loadMultiFilesReal(outputDir, files)
        // TODO find a way to implement it properly
        val jsStdlibPath =
            File(".").absoluteFile.parentFile.parentFile.parentFile.absolutePath + "/" + PathUtil.kotlinPathsForCompiler.jsStdLibKlibPath.toString()
        val compiler = K2JKlibCompiler()

        val messageCollector = MessageCollectorImpl()
        val configuration = CompilerConfiguration().apply {
            put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            put(CLIConfigurationKeys.PERF_MANAGER, compiler.defaultPerformanceManager)
        }

        val arguments = K2JKlibCompilerArguments().apply {
            destination = klibName
            klibLibraries = jsStdlibPath
            freeArgs = sourceFiles
        }

        val rootDisposable = Disposer.newDisposable("Disposable for ${CLICompiler::class.simpleName}.execImpl")
        val paths = computeKotlinPaths(messageCollector, arguments)

        K2JKlibCompiler().compileKlibAndDeserializeIr(
            arguments, configuration, rootDisposable, paths
        )
    }
}
