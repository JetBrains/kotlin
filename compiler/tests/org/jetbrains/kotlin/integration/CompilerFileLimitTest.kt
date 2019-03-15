/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.integration

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

class CompilerFileLimitTest : CompilerSmokeTestBase() {
    fun testLargeKotlinFile() {
        val size = 300

        val dir = tempDir("largeKotlinFileSrc")
        val file = File(dir, "largeKotlinFile.kt")
        file.writeText(generateLargeKotlinFile(size))

        assertIsLargeButNotTooLarge(file)

        runCompiler("largeKotlinFile.compile", file.absolutePath, "-d", tempDir("largeKotlinFileOut").absolutePath)
    }

    private fun generateLargeKotlinFile(size: Int): String {
        return buildString {
            append("package large\n\n")
            (0..size).forEach {
                appendln("class Class$it")
                appendln("{")
                appendln("\tfun foo(): Long = $it")
                appendln("}")
                appendln("\n")
                repeat(2000) {
                    appendln("// kotlin rules ... and stuff")
                }
            }
            appendln("fun main()")
            appendln("{")
            appendln("\tval result = Class5().foo() + Class$size().foo()")
            appendln("\tprintln(result)")
            appendln("}")
        }

    }

    fun testLargeJavaFile() {
        val size = 300

        val dir = tempDir("largeJavaFileSrc")
        val javaDir = File(dir, "large")
        javaDir.mkdir()
        val javaFile = File(javaDir, "Large.java")
        javaFile.writeText(generateLargeJavaFile(size))
        val ktFile = File(dir, "useLargerJava.kt")
        ktFile.writeText(generateKotlinFileThatUsesLargeJavaFile(size))

        assertIsLargeButNotTooLarge(javaFile)

        runCompiler("largeJavaFile.compile", dir.absolutePath, "-d", tempDir("largeJavaFileOut").absolutePath)
    }

    private fun assertIsLargeButNotTooLarge(file: File) {
        Assert.assertTrue(file.length() > 15 * FileUtil.MEGABYTE)
        Assert.assertTrue(file.length() < 20 * FileUtil.MEGABYTE)
    }

    private fun generateKotlinFileThatUsesLargeJavaFile(size: Int): String {
        return buildString {
            append("package usesLarge\n\n")
            append("import large.Large\n\n")
            appendln("fun main()")
            appendln("{")
            appendln("\tval result = Large.Class0().foo() + Large.Class$size().foo()")
            appendln("\tprintln(result)")
            appendln("}")
        }
    }

    private fun generateLargeJavaFile(size: Int): String {
        return buildString {
            append("package large;\n\n")
            appendln("public class Large")
            appendln("{")
            (0..size).forEach {
                appendln("\tpublic static class Class$it")
                appendln("\t{")
                appendln("\t\tpublic long foo()")
                appendln("\t\t{")
                appendln("\t\t\t return $it;")
                appendln("\t\t}")
                appendln("\t}")
                appendln("\n")
                repeat(2000) {
                    appendln("// kotlin rules ... and stuff")
                }
            }
            appendln("}")
        }

    }

    private fun tempDir(markerName: String) = KotlinTestUtils.tmpDir("${CompilerFileLimitTest::class.simpleName}$markerName")
}
