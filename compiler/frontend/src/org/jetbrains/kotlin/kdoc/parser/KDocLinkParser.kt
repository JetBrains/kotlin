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

package org.jetbrains.kotlin.kdoc.parser

import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import com.intellij.lang.PsiBuilder
import com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.JetTokens

/**
 * Parses the contents of a Markdown link in KDoc. Uses the standard Kotlin lexer.
 */
class KDocLinkParser(): PsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        builder.setDebugMode(true)
        val rootMarker = builder.mark()
        if (builder.getTokenType() == JetTokens.LBRACKET) {
            builder.advanceLexer()
        }
        parseQualifiedName(builder)
        if (!builder.eof() && builder.getTokenType() != JetTokens.RBRACKET) {
            builder.error("Closing bracket expected")
            while (!builder.eof() && builder.getTokenType() != JetTokens.RBRACKET) {
                builder.advanceLexer()
            }
        }
        if (builder.getTokenType() == JetTokens.RBRACKET) {
            builder.advanceLexer()
        }
        rootMarker.done(root)
        return builder.getTreeBuilt()
    }

    private fun parseQualifiedName(builder: PsiBuilder) {
        var marker = builder.mark()
        while (true) {
            // don't highlight a word in a link as an error if it happens to be a Kotlin keyword
            if (!isName(builder.getTokenType())) {
                marker.drop()
                builder.error("Identifier expected")
                break
            }
            builder.advanceLexer()
            marker.done(KDocElementTypes.KDOC_NAME)
            if (builder.getTokenType() != JetTokens.DOT) {
                break
            }
            marker = marker.precede()
            builder.advanceLexer()
        }
    }

    private fun isName(tokenType: IElementType) = tokenType == JetTokens.IDENTIFIER || tokenType in JetTokens.KEYWORDS
}
