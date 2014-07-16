/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.findUsages

import org.jetbrains.jet.plugin.search.usagesSearch.*
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.util.Processor
import org.jetbrains.jet.lexer.JetLexer
import com.intellij.lang.cacheBuilder.WordOccurrence
import org.jetbrains.jet.lexer.JetTokens
import com.intellij.lang.cacheBuilder.SimpleWordsScanner
import org.jetbrains.jet.lang.types.expressions.OperatorConventions

class KotlinWordsScanner() : WordsScanner {
    private val lexer = JetLexer()
    private val simpleWordsScanner = SimpleWordsScanner()

    fun scanWords(kind: WordOccurrence.Kind, processor: Processor<WordOccurrence>) {
        val tokenStart = lexer.getTokenStart()
        simpleWordsScanner.processWords(lexer.getTokenSequence()) {
            it!!.init(lexer.getBufferSequence(), tokenStart + it.getStart(), tokenStart + it.getEnd(), kind)
            processor.process(it)
        }
    }

    override fun processWords(fileText: CharSequence, processor: Processor<WordOccurrence>) {
        lexer.start(fileText)

        val occurrence = WordOccurrence(null, 0, 0, null)

        stream { lexer.getTokenType() }.forEach { elementType ->
            // todo: replace with when
            if (ALL_SEARCHABLE_OPERATIONS.contains(elementType)) {
                occurrence.init(lexer.getBufferSequence(), lexer.getTokenStart(), lexer.getTokenEnd(), WordOccurrence.Kind.CODE)
                processor.process(occurrence)
            }
            else if (JetTokens.COMMENTS.contains(elementType)) scanWords(WordOccurrence.Kind.COMMENTS, processor)
            else if (JetTokens.STRINGS.contains(elementType)) scanWords(WordOccurrence.Kind.LITERALS, processor)
            else scanWords(WordOccurrence.Kind.CODE, processor)

            lexer.advance()
        }
    }
}
