/*
 * @author max
 */
package org.jetbrains.jet.lang;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.SyntaxHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetLexer;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.HashMap;
import java.util.Map;

public class JetHighlighter extends SyntaxHighlighterBase {
    private static final Map<IElementType, TextAttributesKey> keys1;
    private static final Map<IElementType, TextAttributesKey> keys2;

    private static final TextAttributesKey JET_KEYWORD = TextAttributesKey.createTextAttributesKey(
                                                  "JET.KEYWORD",
                                                  SyntaxHighlighterColors.KEYWORD.getDefaultAttributes()
                                                 );

    public static final TextAttributesKey JET_SOFT_KEYWORD = TextAttributesKey.createTextAttributesKey(
                                                  "JET.SOFT.KEYWORD",
                                                  SyntaxHighlighterColors.KEYWORD.getDefaultAttributes()
                                                 );

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

    private static final TextAttributesKey JET_STRING = TextAttributesKey.createTextAttributesKey(
                                                 "JET.STRING",
                                                 SyntaxHighlighterColors.STRING.getDefaultAttributes()
                                                );

    private static final TextAttributesKey JET_COMMENT = TextAttributesKey.createTextAttributesKey(
                                                       "JET.COMMENT",
                                                       SyntaxHighlighterColors.LINE_COMMENT.getDefaultAttributes()
                                                     );

    private static final TextAttributesKey JET_BAD_CHARACTER = TextAttributesKey.createTextAttributesKey(
                                                    "JET.BADCHARACTER",
                                                    HighlighterColors.BAD_CHARACTER.getDefaultAttributes()
                                                  );

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


        fillMap(keys1, JetTokens.KEYWORDS, JET_KEYWORD);

        keys1.put(JetTokens.AS_SAFE, JET_KEYWORD);
        keys1.put(JetTokens.LABEL_IDENTIFIER, JET_LABEL_IDENTIFIER);
        keys1.put(JetTokens.ATAT, JET_LABEL_IDENTIFIER);
        keys1.put(JetTokens.FIELD_IDENTIFIER, JET_FIELD_IDENTIFIER);
        keys1.put(JetTokens.INTEGER_LITERAL, JET_NUMBER);
        keys1.put(JetTokens.LONG_LITERAL, JET_NUMBER);
        keys1.put(JetTokens.FLOAT_LITERAL, JET_NUMBER);

        keys1.put(JetTokens.STRING_LITERAL, JET_STRING);
        keys1.put(JetTokens.CHARACTER_LITERAL, JET_STRING);
        keys1.put(JetTokens.RAW_STRING_LITERAL, JET_STRING);

        keys1.put(JetTokens.EOL_COMMENT, JET_COMMENT);
        keys1.put(JetTokens.BLOCK_COMMENT, JET_COMMENT);
        keys1.put(JetTokens.DOC_COMMENT, JET_COMMENT);

        keys1.put(TokenType.BAD_CHARACTER, JET_BAD_CHARACTER);
    }
}
