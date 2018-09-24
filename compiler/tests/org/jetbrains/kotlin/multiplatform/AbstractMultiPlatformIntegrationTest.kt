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
        val commonSrc = File(root, "common.kt").apply { assert(exists()) }
        val jsSrc = File(root, "js.kt").takeIf(File::exists)
        val jvmSrc = File(root, "jvm.kt").takeIf(File::exists)
        // TODO: consider inventing a more clever scheme
        val common2Src = File(root, "common2.kt").takeIf(File::exists)
        val jvm2Src = File(root, "jvm2.kt").takeIf(File::exists)

        val tmpdir = KotlinTestUtils.tmpDir(getTestName(true))

        val optionalStdlibCommon =
            if (InTextDirectivesUtils.isDirectiveDefined(commonSrc.readText(), "WITH_RUNTIME"))
                arrayOf("-cp", findStdlibCommon().absolutePath)
            else emptyArray()

        val commonDest = File(tmpdir, "common").absolutePath
        val jvmDest = File(tmpdir, "jvm").absolutePath.takeIf { jvmSrc != null }
        val jsDest = File(File(tmpdir, "js"), "output.js").absolutePath.takeIf { jsSrc != null }
        val common2Dest = File(tmpdir, "common2").absolutePath.takeIf { common2Src != null }
        val jvm2Dest = File(tmpdir, "jvm2").absolutePath.takeIf { jvm2Src != null }

        val result = buildString {
            appendln("-- Common --")
            appendln(K2MetadataCompiler().compile(commonSrc, null, "-d", commonDest, *optionalStdlibCommon))

            if (jvmSrc != null) {
                appendln()
                appendln("-- JVM --")
                appendln(K2JVMCompiler().compile(jvmSrc, commonSrc, "-d", jvmDest!!))
            }

            if (jsSrc != null) {
                appendln()
                appendln("-- JS --")
                appendln(K2JSCompiler().compile(jsSrc, commonSrc, "-output", jsDest!!))
            }

            if (common2Src != null) {
                appendln()
                appendln("-- Common (2) --")
                appendln(K2MetadataCompiler().compile(common2Src, null, "-d", common2Dest!!, "-cp", commonDest, *optionalStdlibCommon))
            }

            if (jvm2Src != null) {
                appendln()
                appendln("-- JVM (2) --")
                appendln(
                    K2JVMCompiler().compile(
                        jvm2Src, common2Src, "-d", jvm2Dest!!,
                        "-cp", listOfNotNull(commonDest, common2Dest, jvmDest).joinToString(File.pathSeparator)
                    )
                )
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

    private fun CLICompiler<*>.compile(sources: File, commonSources: File?, vararg mainArguments: String): String = buildString {
        val (output, exitCode) = AbstractCliTest.executeCompilerGrabOutput(
            this@compile,
            listOfNotNull(sources.absolutePath, commonSources?.absolutePath, commonSources?.absolutePath?.let("-Xcommon-sources="::plus)) +
                    "-Xmulti-platform" + mainArguments +
                    loadExtraArguments(listOfNotNull(sources, commonSources))
        )
        appendln("Exit code: $exitCode")
        appendln("Output:")
        appendln(output)
    }.trimTrailingWhitespacesAndAddNewlineAtEOF().trimEnd('\r', '\n')

    private fun loadExtraArguments(sources: List<File>): List<String> = sources.flatMap { source ->
        InTextDirectivesUtils.findListWithPrefixes(source.readText(), "// ADDITIONAL_COMPILER_ARGUMENTS:")
    }
}
