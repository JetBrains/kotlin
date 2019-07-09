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

package org.jetbrains.kotlin.generators.builtins.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.generateBuiltIns
import org.junit.Assert
import java.io.PrintWriter
import java.io.StringWriter

class GenerateBuiltInsTest : TestCase() {
    fun testBuiltInsAreUpToDate() {
        generateBuiltIns { file, generator ->
            val sw = StringWriter()
            PrintWriter(sw).use {
                generator(it).generate()
            }

            val expected = StringUtil.convertLineSeparators(sw.toString().trim())
            val actual = StringUtil.convertLineSeparators(FileUtil.loadFile(file).trim())

            Assert.assertEquals("To fix this problem you need to regenerate built-ins (run generateBuiltIns.kt)", expected, actual)
        }
    }
}
