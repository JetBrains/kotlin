/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.backend.common.output.OutputFile

abstract class AbstractCodegenSignatureTest : AbstractCodegenTest() {

    private var isSetup = false
    override fun setUp() {
        isSetup = true
        super.setUp()
    }

    private fun <T> ensureSetup(block: () -> T): T {
        if (!isSetup) setUp()
        return block()
    }

    private fun OutputFile.printApi(): String {
        val text = asText()
        return text
            .splitToSequence("\n")
            .filter {
                if (it.startsWith("  ")) {
                    if (it.startsWith("   ")) false
                    else it[2] != '/' && it[2] != '@'
                } else {
                    it == "}" || it.endsWith("{")
                }
            }
            .joinToString(separator = "\n")
    }

    fun checkApi(src: String, expected: String, dumpClasses: Boolean = false): Unit = ensureSetup {
        val className = "Test_REPLACEME_${uniqueNumber++}"
        val fileName = "$className.kt"

        val loader = classLoader("""
           import androidx.compose.*

           $src
        """, fileName, dumpClasses)

        val apiString = loader
            .allGeneratedFiles
            .filter { it.relativePath.endsWith(".class") }
            .map { it.printApi() }
            .map { it.replace('$', '%') }
            .joinToString(separator = "\n")
            .replace(className, "Test")

        val expectedApiString = expected
            .trimIndent()
            .split("\n")
            .filter { it.isNotBlank() }
            .joinToString("\n")

        assertEquals(expectedApiString, apiString)
    }

    fun codegen(text: String, dumpClasses: Boolean = false): Unit = ensureSetup {
        codegenNoImports(
            """
           import android.content.Context
           import android.widget.*
           import androidx.compose.*

           $text

        """, dumpClasses)
    }

    fun codegenNoImports(text: String, dumpClasses: Boolean = false): Unit = ensureSetup {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"

        classLoader(text, fileName, dumpClasses)
    }
}