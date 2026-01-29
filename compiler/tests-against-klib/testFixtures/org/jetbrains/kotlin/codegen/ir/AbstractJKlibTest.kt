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
        val compiler = K2JKlibCompiler()

        val messageCollector = MessageCollectorImpl()
        val configuration = CompilerConfiguration().apply {
            put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
            put(CommonConfigurationKeys.PERF_MANAGER, compiler.defaultPerformanceManager)
        }

        val arguments = K2JKlibCompilerArguments().apply {
            destination = klibName
            freeArgs = sourceFiles
             
            val stdlibKlibPath = java.security.AccessController.doPrivileged(java.security.PrivilegedAction {
                System.getProperty("kotlin.stdlib.jvm.ir.klib")
            })
            val stdlibJarPath = java.security.AccessController.doPrivileged(java.security.PrivilegedAction {
                System.getProperty("kotlin.stdlib.jvm.ir.jar")
            })
            val reflectJarPath = java.security.AccessController.doPrivileged(java.security.PrivilegedAction {
                System.getProperty("kotlin.reflect.jar")
            })
            val fullStdlibJarPath = java.security.AccessController.doPrivileged(java.security.PrivilegedAction {
                System.getProperty("kotlin.stdlib.jar")
            })
           
            checkNotNull(stdlibKlibPath) { "System property 'kotlin.stdlib.jvm.ir.klib' not found" }
            checkNotNull(stdlibJarPath) { "System property 'kotlin.stdlib.jvm.ir.jar' not found" }
            println("stdlibKlibPath: $stdlibKlibPath")
            println("stdlibJarPath: $stdlibJarPath")
            klibLibraries = stdlibKlibPath
            // klibLibraries = "/Users/joseefort/Code/kotlin/compiler/tests-against-klib/build/stdlib-for-test/kotlin-stdlib-js-2.3.255-SNAPSHOT.klib"
            if (reflectJarPath != null && fullStdlibJarPath != null) {
                 println("Configuring FULL stdlib and reflect")
                 println("Reflect: $reflectJarPath")
                 println("Stdlib: $fullStdlibJarPath")
                 noStdlib = true
                 noReflect = true
                 classpath = "$fullStdlibJarPath${File.pathSeparator}$reflectJarPath"
            } else {
                 println("Configuring MINIMAL stdlib (fallback)")
                 noStdlib = true
                 noReflect = true
                 classpath = stdlibJarPath
            }
            println("FINAL arguments: noStdlib=$noStdlib, noReflect=$noReflect, classpath=$classpath")
        }

        val rootDisposable = Disposer.newDisposable("Disposable for ${CLICompiler::class.simpleName}.execImpl")
        val paths = computeKotlinPaths(messageCollector, arguments)

        try {
            K2JKlibCompiler().compileKlibAndDeserializeIr(
                arguments, configuration, rootDisposable, paths
            )
        } catch (e: Throwable) {
            println(messageCollector.toString())
            throw e
        }

        if (messageCollector.hasErrors()) {
            println(messageCollector.toString())
            throw IllegalStateException("Compilation failed. See errors above.")
        }
    }
}
