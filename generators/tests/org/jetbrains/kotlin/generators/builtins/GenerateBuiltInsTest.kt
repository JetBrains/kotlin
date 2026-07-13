/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.test

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.generators.builtins.generateBuiltIns.generateBuiltIns
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.PrintWriter
import java.io.StringWriter

class GenerateBuiltInsTest {
    @Test
    fun testBuiltInsAreUpToDate() {
        generateBuiltIns { file, generator ->
            val sw = StringWriter()
            PrintWriter(sw).use {
                generator(it).generate()
            }

            val expected = StringUtil.convertLineSeparators(sw.toString().trim())
            val actual = StringUtil.convertLineSeparators(FileUtil.loadFile(file).trim())

            Assertions.assertEquals(expected, actual, "To fix this problem you need to regenerate built-ins (run generateBuiltIns.kt)")
        }
    }
}
