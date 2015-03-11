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

package org.jetbrains.kotlin.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;

public interface JetTokens {
    JetToken EOF   = new JetToken("EOF");

    JetToken BLOCK_COMMENT     = new JetToken("BLOCK_COMMENT");
    JetToken EOL_COMMENT       = new JetToken("EOL_COMMENT");
    JetToken SHEBANG_COMMENT   = new JetToken("SHEBANG_COMMENT");

    //JetToken DOC_COMMENT   = new JetToken("DOC_COMMENT");
    IElementType DOC_COMMENT   = KDocTokens.KDOC;

    IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    JetToken INTEGER_LITERAL    = new JetToken("INTEGER_LITERAL");
    JetToken FLOAT_LITERAL      = new JetToken("FLOAT_CONSTANT");
    JetToken CHARACTER_LITERAL  = new JetToken("CHARACTER_LITERAL");

    JetToken CLOSING_QUOTE = new JetToken("CLOSING_QUOTE");
    JetToken OPEN_QUOTE = new JetToken("OPEN_QUOTE");
    JetToken REGULAR_STRING_PART = new JetToken("REGULAR_STRING_PART");
    JetToken ESCAPE_SEQUENCE = new JetToken("ESCAPE_SEQUENCE");
    JetToken SHORT_TEMPLATE_ENTRY_START = new JetToken("SHORT_TEMPLATE_ENTRY_START");
    JetToken LONG_TEMPLATE_ENTRY_START = new JetToken("LONG_TEMPLATE_ENTRY_START");
    JetToken LONG_TEMPLATE_ENTRY_END = new JetToken("LONG_TEMPLATE_ENTRY_END");
    JetToken DANGLING_NEWLINE = new JetToken("DANGLING_NEWLINE");

    JetKeywordToken PACKAGE_KEYWORD          = JetKeywordToken.keyword("package");
    JetKeywordToken AS_KEYWORD               = JetKeywordToken.keyword("as");
    JetKeywordToken TYPE_ALIAS_KEYWORD       = JetKeywordToken.keyword("typealias");
    JetKeywordToken CLASS_KEYWORD            = JetKeywordToken.keyword("class");
    JetKeywordToken THIS_KEYWORD             = JetKeywordToken.keyword("this");
    JetKeywordToken SUPER_KEYWORD            = JetKeywordToken.keyword("super");
    JetKeywordToken VAL_KEYWORD              = JetKeywordToken.keyword("val");
    JetKeywordToken VAR_KEYWORD              = JetKeywordToken.keyword("var");
    JetKeywordToken FUN_KEYWORD              = JetKeywordToken.keyword("fun");
    JetKeywordToken FOR_KEYWORD              = JetKeywordToken.keyword("for");
    JetKeywordToken NULL_KEYWORD             = JetKeywordToken.keyword("null");
    JetKeywordToken TRUE_KEYWORD             = JetKeywordToken.keyword("true");
    JetKeywordToken FALSE_KEYWORD            = JetKeywordToken.keyword("false");
    JetKeywordToken IS_KEYWORD               = JetKeywordToken.keyword("is");
    JetModifierKeywordToken IN_KEYWORD       = JetModifierKeywordToken.keywordModifier("in");
    JetKeywordToken THROW_KEYWORD            = JetKeywordToken.keyword("throw");
    JetKeywordToken RETURN_KEYWORD           = JetKeywordToken.keyword("return");
    JetKeywordToken BREAK_KEYWORD            = JetKeywordToken.keyword("break");
    JetKeywordToken CONTINUE_KEYWORD         = JetKeywordToken.keyword("continue");
    JetKeywordToken OBJECT_KEYWORD           = JetKeywordToken.keyword("object");
    JetKeywordToken IF_KEYWORD               = JetKeywordToken.keyword("if");
    JetKeywordToken TRY_KEYWORD              = JetKeywordToken.keyword("try");
    JetKeywordToken ELSE_KEYWORD             = JetKeywordToken.keyword("else");
    JetKeywordToken WHILE_KEYWORD            = JetKeywordToken.keyword("while");
    JetKeywordToken DO_KEYWORD               = JetKeywordToken.keyword("do");
    JetKeywordToken WHEN_KEYWORD             = JetKeywordToken.keyword("when");
    JetKeywordToken TRAIT_KEYWORD            = JetKeywordToken.keyword("trait");
    // TODO: Discuss "This" keyword
    JetKeywordToken CAPITALIZED_THIS_KEYWORD = JetKeywordToken.keyword("This");


    JetToken AS_SAFE = JetKeywordToken.keyword("AS_SAFE");//new JetToken("as?");

    JetToken IDENTIFIER = new JetToken("IDENTIFIER");
    JetToken LABEL_IDENTIFIER = new JetToken("LABEL_IDENTIFIER");

    JetToken FIELD_IDENTIFIER = new JetToken("FIELD_IDENTIFIER");
    JetSingleValueToken LBRACKET    = new JetSingleValueToken("LBRACKET", "[");
    JetSingleValueToken RBRACKET    = new JetSingleValueToken("RBRACKET", "]");
    JetSingleValueToken LBRACE      = new JetSingleValueToken("LBRACE", "{");
    JetSingleValueToken RBRACE      = new JetSingleValueToken("RBRACE", "}");
    JetSingleValueToken LPAR        = new JetSingleValueToken("LPAR", "(");
    JetSingleValueToken RPAR        = new JetSingleValueToken("RPAR", ")");
    JetSingleValueToken DOT         = new JetSingleValueToken("DOT", ".");
    JetSingleValueToken PLUSPLUS    = new JetSingleValueToken("PLUSPLUS", "++");
    JetSingleValueToken MINUSMINUS  = new JetSingleValueToken("MINUSMINUS", "--");
    JetSingleValueToken MUL         = new JetSingleValueToken("MUL", "*");
    JetSingleValueToken PLUS        = new JetSingleValueToken("PLUS", "+");
    JetSingleValueToken MINUS       = new JetSingleValueToken("MINUS", "-");
    JetSingleValueToken EXCL        = new JetSingleValueToken("EXCL", "!");
    JetSingleValueToken DIV         = new JetSingleValueToken("DIV", "/");
    JetSingleValueToken PERC        = new JetSingleValueToken("PERC", "%");
    JetSingleValueToken LT          = new JetSingleValueToken("LT", "<");
    JetSingleValueToken GT          = new JetSingleValueToken("GT", ">");
    JetSingleValueToken LTEQ        = new JetSingleValueToken("LTEQ", "<=");
    JetSingleValueToken GTEQ        = new JetSingleValueToken("GTEQ", ">=");
    JetSingleValueToken EQEQEQ      = new JetSingleValueToken("EQEQEQ", "===");
    JetSingleValueToken ARROW       = new JetSingleValueToken("ARROW", "->");
    JetSingleValueToken DOUBLE_ARROW       = new JetSingleValueToken("DOUBLE_ARROW", "=>");
    JetSingleValueToken EXCLEQEQEQ  = new JetSingleValueToken("EXCLEQEQEQ", "!==");
    JetSingleValueToken EQEQ        = new JetSingleValueToken("EQEQ", "==");
    JetSingleValueToken EXCLEQ      = new JetSingleValueToken("EXCLEQ", "!=");
    JetSingleValueToken EXCLEXCL    = new JetSingleValueToken("EXCLEXCL", "!!");
    JetSingleValueToken ANDAND      = new JetSingleValueToken("ANDAND", "&&");
    JetSingleValueToken OROR        = new JetSingleValueToken("OROR", "||");
    JetSingleValueToken SAFE_ACCESS = new JetSingleValueToken("SAFE_ACCESS", "?.");
    JetSingleValueToken ELVIS       = new JetSingleValueToken("ELVIS", "?:");
    JetSingleValueToken QUEST       = new JetSingleValueToken("QUEST", "?");
    JetSingleValueToken COLONCOLON  = new JetSingleValueToken("COLONCOLON", "::");
    JetSingleValueToken COLON       = new JetSingleValueToken("COLON", ":");
    JetSingleValueToken SEMICOLON   = new JetSingleValueToken("SEMICOLON", ";");
    JetSingleValueToken RANGE       = new JetSingleValueToken("RANGE", "..");
    JetSingleValueToken EQ          = new JetSingleValueToken("EQ", "=");
    JetSingleValueToken MULTEQ      = new JetSingleValueToken("MULTEQ", "*=");
    JetSingleValueToken DIVEQ       = new JetSingleValueToken("DIVEQ", "/=");
    JetSingleValueToken PERCEQ      = new JetSingleValueToken("PERCEQ", "%=");
    JetSingleValueToken PLUSEQ      = new JetSingleValueToken("PLUSEQ", "+=");
    JetSingleValueToken MINUSEQ     = new JetSingleValueToken("MINUSEQ", "-=");
    JetKeywordToken NOT_IN      = JetKeywordToken.keyword("NOT_IN", "!in");
    JetKeywordToken NOT_IS      = JetKeywordToken.keyword("NOT_IS", "!is");
    JetSingleValueToken HASH        = new JetSingleValueToken("HASH", "#");

    JetSingleValueToken COMMA       = new JetSingleValueToken("COMMA", ",");

    JetToken EOL_OR_SEMICOLON   = new JetToken("EOL_OR_SEMICOLON");
    JetKeywordToken FILE_KEYWORD    = JetKeywordToken.softKeyword("file");
    JetKeywordToken IMPORT_KEYWORD    = JetKeywordToken.softKeyword("import");
    JetKeywordToken WHERE_KEYWORD     = JetKeywordToken.softKeyword("where");
    JetKeywordToken BY_KEYWORD        = JetKeywordToken.softKeyword("by");
    JetKeywordToken GET_KEYWORD       = JetKeywordToken.softKeyword("get");
    JetKeywordToken SET_KEYWORD       = JetKeywordToken.softKeyword("set");
    JetKeywordToken CONSTRUCTOR_KEYWORD = JetKeywordToken.softKeyword("constructor");
    JetKeywordToken INIT_KEYWORD        = JetKeywordToken.softKeyword("init");

    JetModifierKeywordToken ABSTRACT_KEYWORD  = JetModifierKeywordToken.softKeywordModifier("abstract");
    JetModifierKeywordToken ENUM_KEYWORD      = JetModifierKeywordToken.softKeywordModifier("enum");
    JetModifierKeywordToken OPEN_KEYWORD      = JetModifierKeywordToken.softKeywordModifier("open");
    JetModifierKeywordToken INNER_KEYWORD     = JetModifierKeywordToken.softKeywordModifier("inner");
    JetModifierKeywordToken ANNOTATION_KEYWORD = JetModifierKeywordToken.softKeywordModifier("annotation");
    JetModifierKeywordToken OVERRIDE_KEYWORD  = JetModifierKeywordToken.softKeywordModifier("override");
    JetModifierKeywordToken PRIVATE_KEYWORD   = JetModifierKeywordToken.softKeywordModifier("private");
    JetModifierKeywordToken PUBLIC_KEYWORD    = JetModifierKeywordToken.softKeywordModifier("public");
    JetModifierKeywordToken INTERNAL_KEYWORD  = JetModifierKeywordToken.softKeywordModifier("internal");
    JetModifierKeywordToken PROTECTED_KEYWORD = JetModifierKeywordToken.softKeywordModifier("protected");
    JetKeywordToken CATCH_KEYWORD     = JetKeywordToken.softKeyword("catch");
    JetModifierKeywordToken OUT_KEYWORD       = JetModifierKeywordToken.softKeywordModifier("out");
    JetModifierKeywordToken VARARG_KEYWORD    = JetModifierKeywordToken.softKeywordModifier("vararg");
    JetModifierKeywordToken REIFIED_KEYWORD   = JetModifierKeywordToken.softKeywordModifier("reified");
    JetModifierKeywordToken DYNAMIC_KEYWORD   = JetModifierKeywordToken.softKeywordModifier("dynamic");
    JetModifierKeywordToken DEFAULT_KEYWORD   = JetModifierKeywordToken.softKeywordModifier("default");

    JetKeywordToken FINALLY_KEYWORD   = JetKeywordToken.softKeyword("finally");
    JetModifierKeywordToken FINAL_KEYWORD     = JetModifierKeywordToken.softKeywordModifier("final");

    TokenSet KEYWORDS = TokenSet.create(PACKAGE_KEYWORD, AS_KEYWORD, TYPE_ALIAS_KEYWORD, CLASS_KEYWORD, TRAIT_KEYWORD,
                                        THIS_KEYWORD, SUPER_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD, FOR_KEYWORD,
                                        NULL_KEYWORD,
                                        TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD,
                                        IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD,
                                        ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, TRY_KEYWORD, WHEN_KEYWORD,
                                        NOT_IN, NOT_IS, CAPITALIZED_THIS_KEYWORD, AS_SAFE
    );

    TokenSet SOFT_KEYWORDS = TokenSet.create(FILE_KEYWORD, IMPORT_KEYWORD, WHERE_KEYWORD, BY_KEYWORD, GET_KEYWORD,
                                             SET_KEYWORD, ABSTRACT_KEYWORD, ENUM_KEYWORD, OPEN_KEYWORD, INNER_KEYWORD, ANNOTATION_KEYWORD,
                                             OVERRIDE_KEYWORD, PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD,
                                             CATCH_KEYWORD, FINALLY_KEYWORD, OUT_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD, REIFIED_KEYWORD,
                                             DYNAMIC_KEYWORD, DEFAULT_KEYWORD, CONSTRUCTOR_KEYWORD, INIT_KEYWORD
    );

    /*
        This array is used in stub serialization:
        1. Do not change order.
        2. If you add an entry or change order, increase stub version.
     */
    JetModifierKeywordToken[] MODIFIER_KEYWORDS_ARRAY =
            new JetModifierKeywordToken[] {
                    ABSTRACT_KEYWORD, ENUM_KEYWORD, OPEN_KEYWORD, INNER_KEYWORD, ANNOTATION_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
                    PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, OUT_KEYWORD, IN_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD,
                    REIFIED_KEYWORD, DEFAULT_KEYWORD
            };

    TokenSet MODIFIER_KEYWORDS = TokenSet.create(MODIFIER_KEYWORDS_ARRAY);

    TokenSet VISIBILITY_MODIFIERS = TokenSet.create(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD);

    TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);

    /**
     * Don't add KDocTokens to COMMENTS TokenSet, because it is used in JetParserDefinition.getCommentTokens(),
     * and therefor all COMMENTS tokens will be ignored by PsiBuilder.
     *
     * @see org.jetbrains.kotlin.psi.JetPsiUtil#isInComment(com.intellij.psi.PsiElement)
     */
    TokenSet COMMENTS = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT);
    TokenSet WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(COMMENTS, TokenSet.create(WHITE_SPACE));

    TokenSet STRINGS = TokenSet.create(CHARACTER_LITERAL, REGULAR_STRING_PART);
    TokenSet OPERATIONS = TokenSet.create(AS_KEYWORD, AS_SAFE, IS_KEYWORD, IN_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, EXCLEXCL, MUL, PLUS,
                                          MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR,
                                          SAFE_ACCESS, ELVIS,
            //            MAP, FILTER,
                                          COLON,
                                          RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,
                                          NOT_IN, NOT_IS,
                                          IDENTIFIER, LABEL_IDENTIFIER);

    TokenSet AUGMENTED_ASSIGNMENTS = TokenSet.create(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    TokenSet ALL_ASSIGNMENTS = TokenSet.create(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
}
