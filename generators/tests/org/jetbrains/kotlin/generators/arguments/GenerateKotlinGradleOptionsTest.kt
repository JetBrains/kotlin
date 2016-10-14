/*
 * Copyright 2010-20166 JetBrains s.r.o.
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

package org.jetbrains.kotlin.generators.arguments.test

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.generators.arguments.generateKotlinGradleOptions
import org.jetbrains.kotlin.utils.Printer
import java.io.*

class GenerateKotlinGradleOptionsTest : UsefulTestCase() {
    fun testKotlinGradleOptionsAreUpToDate() {
        fun getPrinter(file: File, fn: Printer.()->Unit) {
            val bytesOut = ByteArrayOutputStream()

            PrintStream(bytesOut).use {
                val printer = Printer(it)
                printer.fn()
            }

            val upToDateContent = bytesOut.toString()
            UsefulTestCase.assertSameLinesWithFile(file.absolutePath, upToDateContent)
        }

        generateKotlinGradleOptions(::getPrinter)
    }
}
