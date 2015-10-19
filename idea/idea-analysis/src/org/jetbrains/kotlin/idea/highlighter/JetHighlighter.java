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

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;
import org.jetbrains.kotlin.lexer.KtTokens;

import java.util.HashMap;
import java.util.Map;

public class JetHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> keys1;
    private static final Map<IElementType, TextAttributesKey> keys2;

    @Override
    @NotNull
    public Lexer getHighlightingLexer() {
        return new JetHighlightingLexer();
    }

    @Override
    @NotNull
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(keys1.get(tokenType), keys2.get(tokenType));
    }

    static {
        keys1 = new HashMap<IElementType, TextAttributesKey>();
        keys2 = new HashMap<IElementType, TextAttributesKey>();

        fillMap(keys1, KtTokens.KEYWORDS, JetHighlightingColors.KEYWORD);

        keys1.put(KtTokens.AS_SAFE, JetHighlightingColors.KEYWORD);
        keys1.put(KtTokens.INTEGER_LITERAL, JetHighlightingColors.NUMBER);
        keys1.put(KtTokens.FLOAT_LITERAL, JetHighlightingColors.NUMBER);

        fillMap(keys1,
                TokenSet.andNot(KtTokens.OPERATIONS,
                                TokenSet.orSet(
                                        TokenSet.create(KtTokens.IDENTIFIER, KtTokens.AT),
                                        KtTokens.KEYWORDS)),
                JetHighlightingColors.OPERATOR_SIGN);

        keys1.put(KtTokens.LPAR, JetHighlightingColors.PARENTHESIS);
        keys1.put(KtTokens.RPAR, JetHighlightingColors.PARENTHESIS);
        keys1.put(KtTokens.LBRACE, JetHighlightingColors.BRACES);
        keys1.put(KtTokens.RBRACE, JetHighlightingColors.BRACES);
        keys1.put(KtTokens.LBRACKET, JetHighlightingColors.BRACKETS);
        keys1.put(KtTokens.RBRACKET, JetHighlightingColors.BRACKETS);
        keys1.put(KtTokens.COMMA, JetHighlightingColors.COMMA);
        keys1.put(KtTokens.SEMICOLON, JetHighlightingColors.SEMICOLON);
        keys1.put(KtTokens.DOT, JetHighlightingColors.DOT);
        keys1.put(KtTokens.ARROW, JetHighlightingColors.ARROW);

        keys1.put(KtTokens.OPEN_QUOTE, JetHighlightingColors.STRING);
        keys1.put(KtTokens.CLOSING_QUOTE, JetHighlightingColors.STRING);
        keys1.put(KtTokens.REGULAR_STRING_PART, JetHighlightingColors.STRING);
        keys1.put(KtTokens.LONG_TEMPLATE_ENTRY_END, JetHighlightingColors.STRING_ESCAPE);
        keys1.put(KtTokens.LONG_TEMPLATE_ENTRY_START, JetHighlightingColors.STRING_ESCAPE);
        keys1.put(KtTokens.SHORT_TEMPLATE_ENTRY_START, JetHighlightingColors.STRING_ESCAPE);

        keys1.put(KtTokens.ESCAPE_SEQUENCE, JetHighlightingColors.STRING_ESCAPE);

        keys1.put(KtTokens.CHARACTER_LITERAL, JetHighlightingColors.STRING);

        keys1.put(KtTokens.EOL_COMMENT, JetHighlightingColors.LINE_COMMENT);
        keys1.put(KtTokens.SHEBANG_COMMENT, JetHighlightingColors.LINE_COMMENT);
        keys1.put(KtTokens.BLOCK_COMMENT, JetHighlightingColors.BLOCK_COMMENT);
        keys1.put(KtTokens.DOC_COMMENT, JetHighlightingColors.DOC_COMMENT);

        fillMap(keys1, KDocTokens.KDOC_HIGHLIGHT_TOKENS, JetHighlightingColors.DOC_COMMENT);
        keys1.put(KDocTokens.TAG_NAME, JetHighlightingColors.KDOC_TAG);
        keys2.put(KDocTokens.TAG_NAME, JetHighlightingColors.DOC_COMMENT);

        keys1.put(TokenType.BAD_CHARACTER, JetHighlightingColors.BAD_CHARACTER);
    }
}
