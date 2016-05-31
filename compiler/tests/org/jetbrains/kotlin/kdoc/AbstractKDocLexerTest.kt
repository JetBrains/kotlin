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

package org.jetbrains.kotlin.kdoc

import com.intellij.lang.TokenWrapper
import com.intellij.lexer.Lexer
import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.kdoc.lexer.KDocLexer
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File

abstract class AbstractKDocLexerTest : TestCase() {
    protected fun doTest(fileName: String) {
        val text = File(fileName).readText()
        val lexerResult = printTokens(text, 0, KDocLexer())
        KtUsefulTestCase.assertSameLinesWithFile(fileName.replaceAfterLast(".", "txt"), lexerResult)
    }

    fun printTokens(text: CharSequence, start: Int, lexer: Lexer): String {
        lexer.start(text, start, text.length)
        var result = ""
        while (true) {
            val tokenType = lexer.tokenType ?: break
            val tokenText = getTokenText(lexer)
            val tokenTypeName = tokenType.toString()
            val line = "$tokenTypeName ('$tokenText')\n"
            result += line
            lexer.advance()
        }
        return result
    }

    private fun getTokenText(lexer: Lexer): String {
        val tokenType = lexer.tokenType
        if (tokenType is TokenWrapper) {
            return tokenType.value
        }

        var text = lexer.bufferSequence.subSequence(lexer.tokenStart, lexer.tokenEnd).toString()
        text = StringUtil.replace(text, "\n", "\\n")
        return text
    }
}