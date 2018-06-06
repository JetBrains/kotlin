/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.multiplatform

import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

abstract class AbstractMultiPlatformIntegrationTest : KtUsefulTestCase() {
    fun doTest(directoryPath: String) {
        val root = File(directoryPath).apply { assert(exists()) }
        val commonSrc = File(root, "common.kt")
        val jsSrc = File(root, "js.kt")
        val jvmSrc = File(root, "jvm.kt")
        // TODO: consider inventing a more clever scheme
        val jvm2Src = File(root, "jvm2.kt")

        val tmpdir = KotlinTestUtils.tmpDir(getTestName(true))

        val optionalStdlibCommon =
                if (InTextDirectivesUtils.isDirectiveDefined(commonSrc.readText(), "WITH_RUNTIME"))
                    arrayOf("-cp", findStdlibCommon().absolutePath)
                else emptyArray()

        val commonDest = File(tmpdir, "common").absolutePath
        val jvmDest = File(tmpdir, "jvm").absolutePath
        val jsDest = File(File(tmpdir, "js"), "output.js").absolutePath
        val jvm2Dest = File(tmpdir, "jvm2").absolutePath

        val result = buildString {
            appendln("-- Common --")
            appendln(K2MetadataCompiler().compile(listOf(commonSrc), "-d", commonDest, *optionalStdlibCommon))

            if (jvmSrc.exists()) {
                appendln()
                appendln("-- JVM --")
                appendln(K2JVMCompiler().compileBothWays(commonSrc, jvmSrc, "-d", jvmDest))
            }

            if (jsSrc.exists()) {
                appendln()
                appendln("-- JS --")
                appendln(K2JSCompiler().compileBothWays(commonSrc, jsSrc, "-output", jsDest))
            }

            if (jvm2Src.exists()) {
                appendln()
                appendln("-- JVM (2) --")
                appendln(K2JVMCompiler().compile(listOf(jvm2Src), "-d", jvm2Dest, "-cp", listOf(commonDest, jvmDest).joinToString(File.pathSeparator)))
            }
        }

        KotlinTestUtils.assertEqualsToFile(File(root, "output.txt"), result.replace('\\', '/'))
    }

    private fun findStdlibCommon(): File {
        // Take kotlin-stdlib-common.jar from dist/ when it's there
        val stdlibCommonLibsDir = "libraries/stdlib/common/build/libs"
        val commonLibs = Files.newDirectoryStream(Paths.get(stdlibCommonLibsDir)).use(Iterable<Path>::toList)
        return commonLibs.sorted().findLast {
            val name = it.toFile().name
            !name.endsWith("-javadoc.jar") && !name.endsWith("-sources.jar") && !name.contains("coroutines")
        }?.toFile() ?: error("kotlin-stdlib-common is not found in $stdlibCommonLibsDir")
    }

    private fun CLICompiler<*>.compileBothWays(commonSource: File, platformSource: File, vararg mainArguments: String): String {
        val configurations = listOf(
                listOf(platformSource, commonSource),
                listOf(commonSource, platformSource)
        )

        val (platformFirst, commonFirst) = configurations.map { compile(it, *mainArguments) }

        if (platformFirst != commonFirst) {
            assertEquals(
                    "Compilation results are different when compiling [platform-specific, common] compared to when compiling [common, platform-specific]",
                    "// Compiling [platform-specific, common]\n\n$platformFirst",
                    "// Compiling [common, platform-specific]\n\n$commonFirst"
            )
        }
        return platformFirst
    }

    private fun CLICompiler<*>.compile(sources: List<File>, vararg mainArguments: String): String = buildString {
        val (output, exitCode) = AbstractCliTest.executeCompilerGrabOutput(
                this@compile,
                sources.map(File::getAbsolutePath) + listOf("-Xmulti-platform") + mainArguments + loadExtraArguments(sources)
        )
        appendln("Exit code: $exitCode")
        appendln("Output:")
        appendln(output)
    }.trimTrailingWhitespacesAndAddNewlineAtEOF().trimEnd('\r', '\n')

    private fun loadExtraArguments(sources: List<File>): List<String> = sources.flatMap { source ->
        InTextDirectivesUtils.findListWithPrefixes(source.readText(), "// ADDITIONAL_COMPILER_ARGUMENTS:")
    }
}
