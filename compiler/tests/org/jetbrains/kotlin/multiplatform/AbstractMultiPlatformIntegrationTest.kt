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
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File

abstract class AbstractMultiPlatformIntegrationTest : KtUsefulTestCase() {
    fun doTest(directoryPath: String) {
        val root = File(directoryPath).apply { assert(exists()) }
        val commonSrc = File(root, "common.kt")
        val jsSrc = File(root, "js.kt")
        val jvmSrc = File(root, "jvm.kt")

        val tmpdir = KotlinTestUtils.tmpDir(getTestName(true))

        val commonDest = File(tmpdir, "common")
        val jvmDest = File(tmpdir, "jvm")
        val jsDest = File(File(tmpdir, "js"), "output.js")

        val result = buildString {
            val (commonOutput, commonExitCode) = AbstractCliTest.executeCompilerGrabOutput(K2MetadataCompiler(), listOf(
                    commonSrc.absolutePath, "-d", commonDest.absolutePath, "-Xskip-java-check"
            ))
            appendln("-- Common --")
            appendln("Exit code: $commonExitCode")
            appendln("Output:")
            appendln(commonOutput)

            if (jvmSrc.exists()) {
                val (jvmOutput, jvmExitCode) = AbstractCliTest.executeCompilerGrabOutput(K2JVMCompiler(), listOf(
                        jvmSrc.absolutePath, commonSrc.absolutePath,
                        "-d", jvmDest.absolutePath,
                        "-Xmulti-platform", "-Xskip-java-check"
                ))
                appendln("-- JVM --")
                appendln("Exit code: $jvmExitCode")
                appendln("Output:")
                appendln(jvmOutput)
            }

            if (jsSrc.exists()) {
                val (jsOutput, jsExitCode) = AbstractCliTest.executeCompilerGrabOutput(K2JSCompiler(), listOf(
                        jsSrc.absolutePath, commonSrc.absolutePath,
                        "-output", jsDest.absolutePath,
                        "-Xmulti-platform", "-Xskip-java-check"
                ))
                appendln("-- JS --")
                appendln("Exit code: $jsExitCode")
                appendln("Output:")
                append(jsOutput)
            }
        }

        KotlinTestUtils.assertEqualsToFile(File(root, "output.txt"), result.replace('\\', '/'))
    }
}
