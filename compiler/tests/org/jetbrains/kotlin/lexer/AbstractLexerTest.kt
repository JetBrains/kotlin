/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lexer

import com.intellij.lang.TokenWrapper
import com.intellij.lexer.Lexer
import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File

abstract class AbstractLexerTest(private val lexer: Lexer) : TestCase() {
    protected fun doTest(fileName: String) {
        val text = File(fileName).readText()
        val lexerResult = printTokens(StringUtil.convertLineSeparators(text), 0, lexer)

        KtUsefulTestCase.assertSameLinesWithFile(fileName.replaceAfterLast(".", "txt"), lexerResult)
    }

    private fun printTokens(text: CharSequence, start: Int, lexer: Lexer): String {
        lexer.start(text, start, text.length)

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
            return tokenType.value

        val result = lexer.bufferSequence.subSequence(lexer.tokenStart, lexer.tokenEnd).toString()

        return StringUtil.replace(result, "\n", "\\n")
    }
}
