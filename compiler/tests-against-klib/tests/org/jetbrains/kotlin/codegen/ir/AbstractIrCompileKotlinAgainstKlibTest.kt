/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment.Companion.createForTests
import org.jetbrains.kotlin.codegen.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.nio.file.Paths
import java.util.*

abstract class AbstractCompileKotlinAgainstKlibTest : AbstractBlackBoxCodegenTest() {
    lateinit var klibName: String
    lateinit var outputDir: File

    override val backend = TargetBackend.JVM_IR

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        outputDir = javaSourcesOutputDirectory
        klibName = Paths.get(outputDir.toString(), wholeFile.name.toString().removeSuffix(".kt")).toString()

        val classpath: MutableList<File> = ArrayList()
        classpath.add(KotlinTestUtils.getAnnotationsJar())
        val configuration = createConfiguration(
            configurationKind, getTestJdkKind(files),
            classpath,
            listOf(outputDir),
            files
        )
        myEnvironment = createForTests(
            testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        setupEnvironment(myEnvironment)


        // All files but last are Klib's sources.
        try {
            compileToKlib(files.dropLast(1))
        } catch (t: Throwable) {
            if (!isIgnoredTarget(wholeFile)) {
                throw t
            }
        }
        super.doMultiFileTest(wholeFile, listOf(files.last()))
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        configuration.put(JVMConfigurationKeys.KLIB_PATHS, listOf(klibName + ".klib"))
    }

    // We need real (as opposed to virtual) files in order to produce a Klib.
    private fun loadMultiFilesReal(files: List<TestFile>): List<String> {
        val dir = outputDir
        return files.map { testFile ->
            assert(testFile.name.endsWith(".kt"))
            val ktFile = File(Paths.get(dir.toString(), testFile.name).toString())
            ktFile.writeText(testFile.content)
            ktFile.toString()
        }
    }

    // For now, while there is no common backend, we generate Klib using
    // the JS_IR compiler.
    private fun compileToKlib(files: List<TestFile>) {
        val sourceFiles = loadMultiFilesReal(files)
        val (output, exitCode) = AbstractCliTest.executeCompilerGrabOutput(
            K2JSCompiler(),
            listOf(
                "-output", klibName,
                "-Xir-produce-klib-file",
                "-libraries", "libraries/stdlib/js-ir/build/classes/kotlin/js/main/"
            ) + sourceFiles
        )
        if (exitCode != ExitCode.OK) {
            throw Exception(output)
        }
    }

    companion object {
        const val TIMEOUT_SECONDS = 10L
    }
}
