/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.kdoc.lexer.KDocTokens;
import org.jetbrains.jet.lexer.JetTokens;

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

        fillMap(keys1, JetTokens.KEYWORDS, JetHighlightingColors.KEYWORD);

        keys1.put(JetTokens.AS_SAFE, JetHighlightingColors.KEYWORD);
        keys1.put(JetTokens.LABEL_IDENTIFIER, JetHighlightingColors.LABEL);
        keys1.put(JetTokens.INTEGER_LITERAL, JetHighlightingColors.NUMBER);
        keys1.put(JetTokens.FLOAT_LITERAL, JetHighlightingColors.NUMBER);

        fillMap(keys1,
                TokenSet.andNot(JetTokens.OPERATIONS,
                                TokenSet.orSet(
                                        TokenSet.create(JetTokens.IDENTIFIER, JetTokens.LABEL_IDENTIFIER),
                                        JetTokens.KEYWORDS)),
                JetHighlightingColors.OPERATOR_SIGN);

        keys1.put(JetTokens.LPAR, JetHighlightingColors.PARENTHESIS);
        keys1.put(JetTokens.RPAR, JetHighlightingColors.PARENTHESIS);
        keys1.put(JetTokens.LBRACE, JetHighlightingColors.BRACES);
        keys1.put(JetTokens.RBRACE, JetHighlightingColors.BRACES);
        keys1.put(JetTokens.LBRACKET, JetHighlightingColors.BRACKETS);
        keys1.put(JetTokens.RBRACKET, JetHighlightingColors.BRACKETS);
        keys1.put(JetTokens.COMMA, JetHighlightingColors.COMMA);
        keys1.put(JetTokens.SEMICOLON, JetHighlightingColors.SEMICOLON);
        keys1.put(JetTokens.DOT, JetHighlightingColors.DOT);
        keys1.put(JetTokens.ARROW, JetHighlightingColors.ARROW);

        keys1.put(JetTokens.OPEN_QUOTE, JetHighlightingColors.STRING);
        keys1.put(JetTokens.CLOSING_QUOTE, JetHighlightingColors.STRING);
        keys1.put(JetTokens.REGULAR_STRING_PART, JetHighlightingColors.STRING);
        keys1.put(JetTokens.LONG_TEMPLATE_ENTRY_END, JetHighlightingColors.STRING_ESCAPE);
        keys1.put(JetTokens.LONG_TEMPLATE_ENTRY_START, JetHighlightingColors.STRING_ESCAPE);
        keys1.put(JetTokens.SHORT_TEMPLATE_ENTRY_START, JetHighlightingColors.STRING_ESCAPE);

        keys1.put(JetTokens.ESCAPE_SEQUENCE, JetHighlightingColors.STRING_ESCAPE);

        keys1.put(JetTokens.CHARACTER_LITERAL, JetHighlightingColors.STRING);

        keys1.put(JetTokens.EOL_COMMENT, JetHighlightingColors.LINE_COMMENT);
        keys1.put(JetTokens.SHEBANG_COMMENT, JetHighlightingColors.LINE_COMMENT);
        keys1.put(JetTokens.BLOCK_COMMENT, JetHighlightingColors.BLOCK_COMMENT);
        keys1.put(JetTokens.DOC_COMMENT, JetHighlightingColors.DOC_COMMENT);

        fillMap(keys1, KDocTokens.KDOC_HIGHLIGHT_TOKENS, JetHighlightingColors.DOC_COMMENT);
        keys1.put(KDocTokens.TAG_NAME, JetHighlightingColors.KDOC_TAG);
        keys2.put(KDocTokens.TAG_NAME, JetHighlightingColors.DOC_COMMENT);

        keys1.put(TokenType.BAD_CHARACTER, JetHighlightingColors.BAD_CHARACTER);
    }
}
