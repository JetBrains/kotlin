/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.multiplatform

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.TestDataAssertions
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.File

abstract class AbstractMultiPlatformIntegrationTest : KtUsefulTestCase() {
    fun doTest(directoryPath: String) {
        val root = File(directoryPath).apply { assert(exists()) }
        val commonSrc = File(root, "common.kt").apply { assert(exists()) }
        val jsSrc = File(root, "js.kt").takeIf(File::exists)
        val jvmSrc = File(root, "jvm.kt").takeIf(File::exists)
        // TODO: consider inventing a more clever scheme
        val common2Src = File(root, "common2.kt").takeIf(File::exists)
        val jvm2Src = File(root, "jvm2.kt").takeIf(File::exists)

        val tmpdir = KtTestUtil.tmpDir(getTestName(true))
        val stdlibCommon = ForTestCompileRuntime.stdlibCommonForTests().absolutePath

        val commonDest = File(tmpdir, "common").absolutePath
        val jvmDest = File(tmpdir, "jvm").absolutePath.takeIf { jvmSrc != null }
        val jsDest = File(tmpdir, "js").absolutePath.takeIf { jsSrc != null }
        val common2Dest = File(tmpdir, "common2").absolutePath.takeIf { common2Src != null }
        val jvm2Dest = File(tmpdir, "jvm2").absolutePath.takeIf { jvm2Src != null }

        val result = buildString {
            appendLine("-- Common --")
            appendLine(KotlinMetadataCompiler().compile(commonSrc, null, "-d", commonDest, "-cp", stdlibCommon, "-Xtarget-platform=JVM,JS,WasmJs,WasmWasi,Native"))

            if (jvmSrc != null) {
                appendLine()
                appendLine("-- JVM --")
                appendLine(K2JVMCompiler().compile(jvmSrc, commonSrc, "-d", jvmDest!!))
            }

            if (jsSrc != null) {
                appendLine()
                appendLine("-- JS --")
                appendLine(
                    K2JSCompiler().compile(
                        jsSrc,
                        commonSrc,
                        "-Xir-produce-klib-dir",
                        "-libraries",
                        ForTestCompileRuntime.stdlibJs().absolutePath,
                        "-ir-output-dir",
                        jsDest!!,
                        "-ir-output-name",
                        "output"
                    )
                )
            }

            if (common2Src != null) {
                appendLine()
                appendLine("-- Common (2) --")
                appendLine(KotlinMetadataCompiler().compile(
                        sources = common2Src,
                        commonSources = null,
                        "-d", common2Dest!!,
                        "-cp", listOf(commonDest, stdlibCommon).joinToString(File.pathSeparator)
                    )
                )
            }

            if (jvm2Src != null) {
                appendLine()
                appendLine("-- JVM (2) --")
                appendLine(
                    K2JVMCompiler().compile(
                        jvm2Src, common2Src, "-d", jvm2Dest!!,
                        "-cp", listOfNotNull(commonDest, common2Dest, jvmDest).joinToString(File.pathSeparator)
                    )
                )
            }
        }

        TestDataAssertions.assertEqualsToFile(File(root, "output.txt"), result.replace('\\', '/'))
    }

    private fun CLICompiler<*>.compile(sources: File, commonSources: File?, vararg mainArguments: String): String = buildString {
        val (output, exitCode) = AbstractCliTest.executeCompilerGrabOutput(
            this@compile,
            listOfNotNull(sources.absolutePath, commonSources?.absolutePath, commonSources?.absolutePath?.let("-Xcommon-sources="::plus)) +
                    "-Xmulti-platform" + mainArguments +
                    loadExtraArguments(listOfNotNull(sources, commonSources))
        )
        appendLine("Exit code: $exitCode")
        appendLine("Output:")
        appendLine(output)
    }.trimTrailingWhitespacesAndAddNewlineAtEOF().trimEnd('\r', '\n')

    private fun loadExtraArguments(sources: List<File>): List<String> = sources.flatMap { source ->
        InTextDirectivesUtils.findListWithPrefixes(source.readText(), "// ADDITIONAL_COMPILER_ARGUMENTS:")
    }
}
