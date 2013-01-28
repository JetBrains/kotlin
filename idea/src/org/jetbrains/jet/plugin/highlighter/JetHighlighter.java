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
import org.jetbrains.jet.lexer.JetLexer;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.HashMap;
import java.util.Map;

public class JetHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> keys;

    @NotNull
    public Lexer getHighlightingLexer() {
        return new JetLexer();
    }

    @NotNull
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(keys.get(tokenType));
    }

    static {
        keys = new HashMap<IElementType, TextAttributesKey>();

        fillMap(keys, JetTokens.KEYWORDS, JetHighlightingColors.KEYWORD);

        keys.put(JetTokens.AS_SAFE, JetHighlightingColors.KEYWORD);
        keys.put(JetTokens.LABEL_IDENTIFIER, JetHighlightingColors.LABEL);
        keys.put(JetTokens.ATAT, JetHighlightingColors.LABEL);
        keys.put(JetTokens.INTEGER_LITERAL, JetHighlightingColors.NUMBER);
        keys.put(JetTokens.FLOAT_LITERAL, JetHighlightingColors.NUMBER);

        fillMap(keys, JetTokens.OPERATIONS.minus(
            TokenSet.create(JetTokens.IDENTIFIER, JetTokens.LABEL_IDENTIFIER)).minus(
            JetTokens.KEYWORDS), JetHighlightingColors.OPERATOR_SIGN);
        keys.put(JetTokens.LPAR, JetHighlightingColors.PARENTHESIS);
        keys.put(JetTokens.RPAR, JetHighlightingColors.PARENTHESIS);
        keys.put(JetTokens.LBRACE, JetHighlightingColors.BRACES);
        keys.put(JetTokens.RBRACE, JetHighlightingColors.BRACES);
        keys.put(JetTokens.LBRACKET, JetHighlightingColors.BRACKETS);
        keys.put(JetTokens.RBRACKET, JetHighlightingColors.BRACKETS);
        keys.put(JetTokens.COMMA, JetHighlightingColors.COMMA);
        keys.put(JetTokens.SEMICOLON, JetHighlightingColors.SEMICOLON);
        keys.put(JetTokens.DOT, JetHighlightingColors.DOT);
        keys.put(JetTokens.ARROW, JetHighlightingColors.ARROW);

        keys.put(JetTokens.OPEN_QUOTE, JetHighlightingColors.STRING);
        keys.put(JetTokens.CLOSING_QUOTE, JetHighlightingColors.STRING);
        keys.put(JetTokens.REGULAR_STRING_PART, JetHighlightingColors.STRING);
        keys.put(JetTokens.LONG_TEMPLATE_ENTRY_END, JetHighlightingColors.STRING_ESCAPE);
        keys.put(JetTokens.LONG_TEMPLATE_ENTRY_START, JetHighlightingColors.STRING_ESCAPE);
        keys.put(JetTokens.SHORT_TEMPLATE_ENTRY_START, JetHighlightingColors.STRING_ESCAPE);

        keys.put(JetTokens.ESCAPE_SEQUENCE, JetHighlightingColors.STRING_ESCAPE);

        keys.put(JetTokens.CHARACTER_LITERAL, JetHighlightingColors.STRING);

        keys.put(JetTokens.EOL_COMMENT, JetHighlightingColors.LINE_COMMENT);
        keys.put(JetTokens.SHEBANG_COMMENT, JetHighlightingColors.LINE_COMMENT);
        keys.put(JetTokens.BLOCK_COMMENT, JetHighlightingColors.BLOCK_COMMENT);
        keys.put(JetTokens.DOC_COMMENT, JetHighlightingColors.DOC_COMMENT);

        keys.put(TokenType.BAD_CHARACTER, JetHighlightingColors.BAD_CHARACTER);
    }
}
