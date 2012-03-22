/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package org.jetbrains.jet.plugin.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.SyntaxHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.Colors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetLexer;
import org.jetbrains.jet.lexer.JetTokens;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class JetHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> keys1;
    private static final Map<IElementType, TextAttributesKey> keys2;

    public static final TextAttributesKey JET_SOFT_KEYWORD;
    static {
        TextAttributes attributes = SyntaxHighlighterColors.KEYWORD.getDefaultAttributes().clone();
        attributes.setForegroundColor(Colors.DARK_RED);
        JET_SOFT_KEYWORD = TextAttributesKey.createTextAttributesKey(
                                                  "JET.SOFT.KEYWORD",
                                                  attributes
                                                 );
    }

    public static final TextAttributesKey JET_FIELD_IDENTIFIER = TextAttributesKey.createTextAttributesKey(
                                                  "JET.FIELD.IDENTIFIER",
// TODO: proper attributes
                                                  SyntaxHighlighterColors.NUMBER.getDefaultAttributes()
                                                 );

    public static final TextAttributesKey JET_PROPERTY_WITH_BACKING_FIELD_IDENTIFIER = TextAttributesKey.createTextAttributesKey(
                                                  "JET.PROPERTY.WITH.BACKING.FIELD.IDENTIFIER",
// TODO: proper attributes
                                                  SyntaxHighlighterColors.NUMBER.getDefaultAttributes()
                                                 );

    public static final TextAttributesKey JET_LABEL_IDENTIFIER = TextAttributesKey.createTextAttributesKey(
                                                  "JET.LABEL.IDENTIFIER",
// TODO: proper attributes
                                                  SyntaxHighlighterColors.NUMBER.getDefaultAttributes()
                                                 );

    private static final TextAttributesKey JET_NUMBER = TextAttributesKey.createTextAttributesKey(
                                                 "JET.NUMBER",
                                                 SyntaxHighlighterColors.NUMBER.getDefaultAttributes()
                                                );

    public static final TextAttributesKey JET_STRING = TextAttributesKey.createTextAttributesKey(
                                                 "JET.STRING",
                                                 SyntaxHighlighterColors.STRING.getDefaultAttributes()
                                                );

    public static final TextAttributesKey JET_STRING_ESCAPE = TextAttributesKey.createTextAttributesKey(
                                                 "JET.STRING.ESCAPE",
                                                 SyntaxHighlighterColors.VALID_STRING_ESCAPE.getDefaultAttributes()
                                                );

    private static final TextAttributesKey JET_COMMENT;
    static {
        TextAttributes attributes = SyntaxHighlighterColors.LINE_COMMENT.getDefaultAttributes().clone();
        attributes.setFontType(Font.PLAIN);
        JET_COMMENT = TextAttributesKey.createTextAttributesKey(
                "JET.COMMENT",
                attributes
        );
    }

    private static final TextAttributesKey JET_BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
                                                    "JET.BADCHARACTER",
                                                    HighlighterColors.BAD_CHARACTER.getDefaultAttributes()
                                                  );

    public static final TextAttributesKey JET_AUTO_CAST_EXPRESSION;

    static {
        TextAttributes clone = SyntaxHighlighterColors.STRING.getDefaultAttributes().clone();
        clone.setFontType(Font.PLAIN);
// TODO: proper attributes
        JET_AUTO_CAST_EXPRESSION = TextAttributesKey.createTextAttributesKey("JET.AUTO.CAST.EXPRESSION", clone);
    }

    public static final TextAttributesKey JET_WRAPPED_INTO_REF;

    static {
        TextAttributes attributes = new TextAttributes();
        attributes.setEffectType(EffectType.LINE_UNDERSCORE);
        attributes.setEffectColor(Color.BLACK);
        JET_WRAPPED_INTO_REF = TextAttributesKey.createTextAttributesKey("JET.WRAPPED.INTO.REF", attributes);
    }

    public static final TextAttributesKey JET_AUTOCREATED_IT;

    static {
        TextAttributes attributes = new TextAttributes();
        attributes.setFontType(Font.BOLD);
// TODO: proper attributes
        JET_AUTOCREATED_IT = TextAttributesKey.createTextAttributesKey("JET.AUTO.CREATED.IT", attributes);
    }

    public static final TextAttributesKey JET_FUNCTION_LITERAL_DELIMITER;

    static {
        TextAttributes attributes = new TextAttributes();
        attributes.setFontType(Font.BOLD);
// TODO: proper attributes
        JET_FUNCTION_LITERAL_DELIMITER = TextAttributesKey.createTextAttributesKey("JET.AUTO.CREATED.IT", attributes);
    }

    public static final TextAttributesKey JET_DEBUG_INFO;

    static {
        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setEffectType(EffectType.ROUNDED_BOX);
        textAttributes.setEffectColor(Color.BLACK);
        JET_DEBUG_INFO = TextAttributesKey.createTextAttributesKey("JET.DEBUG.INFO", textAttributes);
    }

    public static final TextAttributesKey JET_RESOLVED_TO_ERROR;

    static {
        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setEffectType(EffectType.ROUNDED_BOX);
        textAttributes.setEffectColor(Color.RED);
        JET_RESOLVED_TO_ERROR = TextAttributesKey.createTextAttributesKey("JET.RESOLVED.TO.ERROR", textAttributes);
    }

    @NotNull
    public Lexer getHighlightingLexer() {
        return new JetLexer();
    }

    @NotNull
    public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
        return pack(keys1.get(tokenType), keys2.get(tokenType));
    }

    static {
        keys1 = new HashMap<IElementType, TextAttributesKey>();
        keys2 = new HashMap<IElementType, TextAttributesKey>();


        fillMap(keys1, JetTokens.KEYWORDS, JetHighlightingColors.JET_KEYWORD);

        keys1.put(JetTokens.AS_SAFE, JetHighlightingColors.JET_KEYWORD);
        keys1.put(JetTokens.LABEL_IDENTIFIER, JET_LABEL_IDENTIFIER);
        keys1.put(JetTokens.ATAT, JET_LABEL_IDENTIFIER);
        keys1.put(JetTokens.FIELD_IDENTIFIER, JET_FIELD_IDENTIFIER);
        keys1.put(JetTokens.INTEGER_LITERAL, JET_NUMBER);
        keys1.put(JetTokens.FLOAT_LITERAL, JET_NUMBER);

        keys1.put(JetTokens.OPEN_QUOTE, JET_STRING);
        keys1.put(JetTokens.CLOSING_QUOTE, JET_STRING);
        keys1.put(JetTokens.REGULAR_STRING_PART, JET_STRING);
        keys1.put(JetTokens.LONG_TEMPLATE_ENTRY_END, JET_STRING_ESCAPE);
        keys1.put(JetTokens.LONG_TEMPLATE_ENTRY_START, JET_STRING_ESCAPE);
        keys1.put(JetTokens.SHORT_TEMPLATE_ENTRY_START, JET_STRING_ESCAPE);

        keys1.put(JetTokens.ESCAPE_SEQUENCE, JET_STRING_ESCAPE);

        keys1.put(JetTokens.CHARACTER_LITERAL, JET_STRING);

        keys1.put(JetTokens.EOL_COMMENT, JET_COMMENT);
        keys1.put(JetTokens.BLOCK_COMMENT, JET_COMMENT);
        keys1.put(JetTokens.DOC_COMMENT, JET_COMMENT);

        keys1.put(TokenType.BAD_CHARACTER, JET_BAD_CHARACTER);
    }
}
