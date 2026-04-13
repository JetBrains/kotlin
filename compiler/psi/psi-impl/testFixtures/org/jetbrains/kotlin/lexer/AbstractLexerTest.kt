/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lexer

import com.intellij.lang.TokenWrapper
import com.intellij.lexer.Lexer
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest
import org.jetbrains.kotlin.analysis.test.data.manager.assertEqualsToTestDataFile
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import kotlin.io.path.readText

abstract class AbstractLexerTest(private val lexer: Lexer) : ManagedTest {
    protected fun runTest(testDataFilePath: String) {
        val testDataFile = ForTestCompileRuntime.transformTestDataPath(testDataFilePath).toPath()
        val text = testDataFile.readText()
        val lexerResult = printTokens(StringUtil.convertLineSeparators(text), lexer)

        assertEqualsToTestDataFile(testDataFile, lexerResult, "txt")
    }

    private fun printTokens(text: CharSequence, lexer: Lexer): String {
        lexer.start(text)

        return buildString {
            while (true) {
                val tokenType = lexer.tokenType ?: break
                append("$tokenType ('${getTokenText(lexer)}')\n")
                lexer.advance()
            }
        }
    }

    private fun getTokenText(lexer: Lexer): String {
        val tokenType = lexer.tokenType

        if (tokenType is TokenWrapper)
            return tokenType.text

        val result = lexer.bufferSequence.subSequence(lexer.tokenStart, lexer.tokenEnd).toString()

        return StringUtil.replace(result, "\n", "\\n")
    }
}
