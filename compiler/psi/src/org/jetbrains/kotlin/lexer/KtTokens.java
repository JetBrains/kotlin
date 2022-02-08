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
import org.jetbrains.kotlin.psi.KtPsiUtil;

import static org.jetbrains.kotlin.KtNodeTypes.DOT_QUALIFIED_EXPRESSION;
import static org.jetbrains.kotlin.KtNodeTypes.SAFE_ACCESS_EXPRESSION;

public interface KtTokens {
    KtToken EOF   = new KtToken("EOF");

    KtToken RESERVED    = new KtToken("RESERVED");

    KtToken BLOCK_COMMENT     = new KtToken("BLOCK_COMMENT");
    KtToken EOL_COMMENT       = new KtToken("EOL_COMMENT");
    KtToken SHEBANG_COMMENT   = new KtToken("SHEBANG_COMMENT");

    //KtToken DOC_COMMENT   = new KtToken("DOC_COMMENT");
    IElementType DOC_COMMENT   = KDocTokens.KDOC;

    IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    KtToken INTEGER_LITERAL    = new KtToken("INTEGER_LITERAL");
    KtToken FLOAT_LITERAL      = new KtToken("FLOAT_CONSTANT");
    KtToken CHARACTER_LITERAL  = new KtToken("CHARACTER_LITERAL");

    KtToken CLOSING_QUOTE = new KtToken("CLOSING_QUOTE");
    KtToken OPEN_QUOTE = new KtToken("OPEN_QUOTE");
    KtToken REGULAR_STRING_PART = new KtToken("REGULAR_STRING_PART");
    KtToken ESCAPE_SEQUENCE = new KtToken("ESCAPE_SEQUENCE");
    KtToken SHORT_TEMPLATE_ENTRY_START = new KtToken("SHORT_TEMPLATE_ENTRY_START");
    KtToken LONG_TEMPLATE_ENTRY_START = new KtToken("LONG_TEMPLATE_ENTRY_START");
    KtToken LONG_TEMPLATE_ENTRY_END = new KtToken("LONG_TEMPLATE_ENTRY_END");
    KtToken DANGLING_NEWLINE = new KtToken("DANGLING_NEWLINE");

    KtKeywordToken PACKAGE_KEYWORD          = KtKeywordToken.keyword("package");
    KtKeywordToken AS_KEYWORD               = KtKeywordToken.keyword("as");
    KtKeywordToken TYPE_ALIAS_KEYWORD       = KtKeywordToken.keyword("typealias");
    KtKeywordToken CLASS_KEYWORD            = KtKeywordToken.keyword("class");
    KtKeywordToken THIS_KEYWORD             = KtKeywordToken.keyword("this");
    KtKeywordToken SUPER_KEYWORD            = KtKeywordToken.keyword("super");
    KtKeywordToken VAL_KEYWORD              = KtKeywordToken.keyword("val");
    KtKeywordToken VAR_KEYWORD              = KtKeywordToken.keyword("var");
    KtModifierKeywordToken FUN_KEYWORD      = KtModifierKeywordToken.keywordModifier("fun");
    KtKeywordToken FOR_KEYWORD              = KtKeywordToken.keyword("for");
    KtKeywordToken NULL_KEYWORD             = KtKeywordToken.keyword("null");
    KtKeywordToken TRUE_KEYWORD             = KtKeywordToken.keyword("true");
    KtKeywordToken FALSE_KEYWORD            = KtKeywordToken.keyword("false");
    KtKeywordToken IS_KEYWORD               = KtKeywordToken.keyword("is");
    KtModifierKeywordToken IN_KEYWORD       = KtModifierKeywordToken.keywordModifier("in");
    KtKeywordToken THROW_KEYWORD            = KtKeywordToken.keyword("throw");
    KtKeywordToken RETURN_KEYWORD           = KtKeywordToken.keyword("return");
    KtKeywordToken BREAK_KEYWORD            = KtKeywordToken.keyword("break");
    KtKeywordToken CONTINUE_KEYWORD         = KtKeywordToken.keyword("continue");
    KtKeywordToken OBJECT_KEYWORD           = KtKeywordToken.keyword("object");
    KtKeywordToken IF_KEYWORD               = KtKeywordToken.keyword("if");
    KtKeywordToken TRY_KEYWORD              = KtKeywordToken.keyword("try");
    KtKeywordToken ELSE_KEYWORD             = KtKeywordToken.keyword("else");
    KtKeywordToken WHILE_KEYWORD            = KtKeywordToken.keyword("while");
    KtKeywordToken DO_KEYWORD               = KtKeywordToken.keyword("do");
    KtKeywordToken WHEN_KEYWORD             = KtKeywordToken.keyword("when");
    KtKeywordToken INTERFACE_KEYWORD        = KtKeywordToken.keyword("interface");

    // Reserved for future use:
    KtKeywordToken TYPEOF_KEYWORD           = KtKeywordToken.keyword("typeof");

    KtToken AS_SAFE = KtKeywordToken.keyword("AS_SAFE");//new KtToken("as?");

    KtToken IDENTIFIER = new KtToken("IDENTIFIER");

    KtToken FIELD_IDENTIFIER = new KtToken("FIELD_IDENTIFIER");
    KtSingleValueToken LBRACKET    = new KtSingleValueToken("LBRACKET", "[");
    KtSingleValueToken RBRACKET    = new KtSingleValueToken("RBRACKET", "]");
    KtSingleValueToken LBRACE      = new KtSingleValueToken("LBRACE", "{");
    KtSingleValueToken RBRACE      = new KtSingleValueToken("RBRACE", "}");
    KtSingleValueToken LPAR        = new KtSingleValueToken("LPAR", "(");
    KtSingleValueToken RPAR        = new KtSingleValueToken("RPAR", ")");
    KtSingleValueToken DOT         = new KtSingleValueToken("DOT", ".");
    KtSingleValueToken PLUSPLUS    = new KtSingleValueToken("PLUSPLUS", "++");
    KtSingleValueToken MINUSMINUS  = new KtSingleValueToken("MINUSMINUS", "--");
    KtSingleValueToken MUL         = new KtSingleValueToken("MUL", "*");
    KtSingleValueToken PLUS        = new KtSingleValueToken("PLUS", "+");
    KtSingleValueToken MINUS       = new KtSingleValueToken("MINUS", "-");
    KtSingleValueToken EXCL        = new KtSingleValueToken("EXCL", "!");
    KtSingleValueToken DIV         = new KtSingleValueToken("DIV", "/");
    KtSingleValueToken PERC        = new KtSingleValueToken("PERC", "%");
    KtSingleValueToken LT          = new KtSingleValueToken("LT", "<");
    KtSingleValueToken GT          = new KtSingleValueToken("GT", ">");
    KtSingleValueToken LTEQ        = new KtSingleValueToken("LTEQ", "<=");
    KtSingleValueToken GTEQ        = new KtSingleValueToken("GTEQ", ">=");
    KtSingleValueToken EQEQEQ      = new KtSingleValueToken("EQEQEQ", "===");
    KtSingleValueToken ARROW       = new KtSingleValueToken("ARROW", "->");
    KtSingleValueToken DOUBLE_ARROW       = new KtSingleValueToken("DOUBLE_ARROW", "=>");
    KtSingleValueToken EXCLEQEQEQ  = new KtSingleValueToken("EXCLEQEQEQ", "!==");
    KtSingleValueToken EQEQ        = new KtSingleValueToken("EQEQ", "==");
    KtSingleValueToken EXCLEQ      = new KtSingleValueToken("EXCLEQ", "!=");
    KtSingleValueToken EXCLEXCL    = new KtSingleValueToken("EXCLEXCL", "!!");
    KtSingleValueToken ANDAND      = new KtSingleValueToken("ANDAND", "&&");
    KtSingleValueToken AND         = new KtSingleValueToken("AND", "&");
    KtSingleValueToken OROR        = new KtSingleValueToken("OROR", "||");
    KtSingleValueToken SAFE_ACCESS = new KtSingleValueToken("SAFE_ACCESS", "?.");
    KtSingleValueToken ELVIS       = new KtSingleValueToken("ELVIS", "?:");
    KtSingleValueToken QUEST       = new KtSingleValueToken("QUEST", "?");
    KtSingleValueToken COLONCOLON  = new KtSingleValueToken("COLONCOLON", "::");
    KtSingleValueToken COLON       = new KtSingleValueToken("COLON", ":");
    KtSingleValueToken SEMICOLON   = new KtSingleValueToken("SEMICOLON", ";");
    KtSingleValueToken DOUBLE_SEMICOLON   = new KtSingleValueToken("DOUBLE_SEMICOLON", ";;");
    KtSingleValueToken RANGE       = new KtSingleValueToken("RANGE", "..");
    KtSingleValueToken EQ          = new KtSingleValueToken("EQ", "=");
    KtSingleValueToken MULTEQ      = new KtSingleValueToken("MULTEQ", "*=");
    KtSingleValueToken DIVEQ       = new KtSingleValueToken("DIVEQ", "/=");
    KtSingleValueToken PERCEQ      = new KtSingleValueToken("PERCEQ", "%=");
    KtSingleValueToken PLUSEQ      = new KtSingleValueToken("PLUSEQ", "+=");
    KtSingleValueToken MINUSEQ     = new KtSingleValueToken("MINUSEQ", "-=");
    KtKeywordToken NOT_IN      = KtKeywordToken.keyword("NOT_IN", "!in");
    KtKeywordToken NOT_IS      = KtKeywordToken.keyword("NOT_IS", "!is");
    KtSingleValueToken HASH        = new KtSingleValueToken("HASH", "#");
    KtSingleValueToken AT          = new KtSingleValueToken("AT", "@");

    KtSingleValueToken COMMA       = new KtSingleValueToken("COMMA", ",");

    KtToken EOL_OR_SEMICOLON   = new KtToken("EOL_OR_SEMICOLON");
    KtKeywordToken FILE_KEYWORD    = KtKeywordToken.softKeyword("file");
    KtKeywordToken FIELD_KEYWORD     = KtKeywordToken.softKeyword("field");
    KtKeywordToken PROPERTY_KEYWORD     = KtKeywordToken.softKeyword("property");
    KtKeywordToken RECEIVER_KEYWORD     = KtKeywordToken.softKeyword("receiver");
    KtKeywordToken PARAM_KEYWORD     = KtKeywordToken.softKeyword("param");
    KtKeywordToken SETPARAM_KEYWORD  = KtKeywordToken.softKeyword("setparam");
    KtKeywordToken DELEGATE_KEYWORD  = KtKeywordToken.softKeyword("delegate");
    KtKeywordToken IMPORT_KEYWORD    = KtKeywordToken.softKeyword("import");
    KtKeywordToken WHERE_KEYWORD     = KtKeywordToken.softKeyword("where");
    KtKeywordToken BY_KEYWORD        = KtKeywordToken.softKeyword("by");
    KtKeywordToken GET_KEYWORD       = KtKeywordToken.softKeyword("get");
    KtKeywordToken SET_KEYWORD       = KtKeywordToken.softKeyword("set");
    KtKeywordToken CONSTRUCTOR_KEYWORD = KtKeywordToken.softKeyword("constructor");
    KtKeywordToken INIT_KEYWORD        = KtKeywordToken.softKeyword("init");
    KtKeywordToken CONTEXT_KEYWORD     = KtKeywordToken.softKeyword("context");

    KtModifierKeywordToken ABSTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("abstract");
    KtModifierKeywordToken ENUM_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("enum");
    KtModifierKeywordToken CONTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("contract");
    KtModifierKeywordToken OPEN_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("open");
    KtModifierKeywordToken INNER_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("inner");
    KtModifierKeywordToken OVERRIDE_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("override");
    KtModifierKeywordToken PRIVATE_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("private");
    KtModifierKeywordToken PUBLIC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("public");
    KtModifierKeywordToken INTERNAL_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("internal");
    KtModifierKeywordToken PROTECTED_KEYWORD = KtModifierKeywordToken.softKeywordModifier("protected");
    KtKeywordToken CATCH_KEYWORD     = KtKeywordToken.softKeyword("catch");
    KtModifierKeywordToken OUT_KEYWORD       = KtModifierKeywordToken.softKeywordModifier("out");
    KtModifierKeywordToken VARARG_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("vararg");
    KtModifierKeywordToken REIFIED_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("reified");
    KtKeywordToken DYNAMIC_KEYWORD   = KtKeywordToken.softKeyword("dynamic");
    KtModifierKeywordToken COMPANION_KEYWORD = KtModifierKeywordToken.softKeywordModifier("companion");
    KtModifierKeywordToken SEALED_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("sealed");

    KtModifierKeywordToken DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD;

    KtKeywordToken FINALLY_KEYWORD   = KtKeywordToken.softKeyword("finally");
    KtModifierKeywordToken FINAL_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("final");

    KtModifierKeywordToken LATEINIT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("lateinit");

    KtModifierKeywordToken DATA_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("data");
    KtModifierKeywordToken VALUE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("value");
    KtModifierKeywordToken INLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("inline");
    KtModifierKeywordToken NOINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("noinline");
    KtModifierKeywordToken TAILREC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("tailrec");
    KtModifierKeywordToken EXTERNAL_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("external");
    KtModifierKeywordToken ANNOTATION_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("annotation");
    KtModifierKeywordToken CROSSINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("crossinline");
    KtModifierKeywordToken OPERATOR_KEYWORD = KtModifierKeywordToken.softKeywordModifier("operator");
    KtModifierKeywordToken INFIX_KEYWORD = KtModifierKeywordToken.softKeywordModifier("infix");

    KtModifierKeywordToken CONST_KEYWORD = KtModifierKeywordToken.softKeywordModifier("const");

    KtModifierKeywordToken SUSPEND_KEYWORD = KtModifierKeywordToken.softKeywordModifier("suspend");

    KtModifierKeywordToken HEADER_KEYWORD = KtModifierKeywordToken.softKeywordModifier("header");
    KtModifierKeywordToken IMPL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("impl");

    KtModifierKeywordToken EXPECT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("expect");
    KtModifierKeywordToken ACTUAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("actual");

    TokenSet KEYWORDS = TokenSet.create(PACKAGE_KEYWORD, AS_KEYWORD, TYPE_ALIAS_KEYWORD, CLASS_KEYWORD, INTERFACE_KEYWORD,
                                        THIS_KEYWORD, SUPER_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD, FOR_KEYWORD,
                                        NULL_KEYWORD,
                                        TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD,
                                        IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD,
                                        ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, TRY_KEYWORD, WHEN_KEYWORD,
                                        NOT_IN, NOT_IS, AS_SAFE,
                                        TYPEOF_KEYWORD
    );

    TokenSet SOFT_KEYWORDS = TokenSet.create(FILE_KEYWORD, IMPORT_KEYWORD, WHERE_KEYWORD, BY_KEYWORD, GET_KEYWORD,
                                             SET_KEYWORD, ABSTRACT_KEYWORD, ENUM_KEYWORD, CONTRACT_KEYWORD, OPEN_KEYWORD, INNER_KEYWORD,
                                             OVERRIDE_KEYWORD, PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD,
                                             CATCH_KEYWORD, FINALLY_KEYWORD, OUT_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD, REIFIED_KEYWORD,
                                             DYNAMIC_KEYWORD, COMPANION_KEYWORD, CONSTRUCTOR_KEYWORD, INIT_KEYWORD, SEALED_KEYWORD,
                                             FIELD_KEYWORD, PROPERTY_KEYWORD, RECEIVER_KEYWORD, PARAM_KEYWORD, SETPARAM_KEYWORD,
                                             DELEGATE_KEYWORD,
                                             LATEINIT_KEYWORD,
                                             DATA_KEYWORD, INLINE_KEYWORD, NOINLINE_KEYWORD, TAILREC_KEYWORD, EXTERNAL_KEYWORD,
                                             ANNOTATION_KEYWORD, CROSSINLINE_KEYWORD, CONST_KEYWORD, OPERATOR_KEYWORD, INFIX_KEYWORD,
                                             SUSPEND_KEYWORD, HEADER_KEYWORD, IMPL_KEYWORD, EXPECT_KEYWORD, ACTUAL_KEYWORD,
                                             VALUE_KEYWORD, CONTEXT_KEYWORD
    );

    /*
        This array is used in stub serialization:
        1. Do not change order.
        2. If you add an entry or change order, increase stub version.
     */
    KtModifierKeywordToken[] MODIFIER_KEYWORDS_ARRAY =
            new KtModifierKeywordToken[] {
                    ABSTRACT_KEYWORD, ENUM_KEYWORD, CONTRACT_KEYWORD, OPEN_KEYWORD, INNER_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
                    PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, OUT_KEYWORD, IN_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD,
                    REIFIED_KEYWORD, COMPANION_KEYWORD, SEALED_KEYWORD, LATEINIT_KEYWORD,
                    DATA_KEYWORD, INLINE_KEYWORD, NOINLINE_KEYWORD, TAILREC_KEYWORD, EXTERNAL_KEYWORD, ANNOTATION_KEYWORD, CROSSINLINE_KEYWORD,
                    CONST_KEYWORD, OPERATOR_KEYWORD, INFIX_KEYWORD, SUSPEND_KEYWORD,
                    HEADER_KEYWORD, IMPL_KEYWORD, EXPECT_KEYWORD, ACTUAL_KEYWORD, FUN_KEYWORD, VALUE_KEYWORD
            };

    TokenSet MODIFIER_KEYWORDS = TokenSet.create(MODIFIER_KEYWORDS_ARRAY);

    TokenSet TYPE_MODIFIER_KEYWORDS = TokenSet.create(SUSPEND_KEYWORD);
    TokenSet TYPE_ARGUMENT_MODIFIER_KEYWORDS = TokenSet.create(IN_KEYWORD, OUT_KEYWORD);
    TokenSet RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS = TokenSet.create(OUT_KEYWORD, VARARG_KEYWORD);

    TokenSet VISIBILITY_MODIFIERS = TokenSet.create(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD);
    TokenSet MODALITY_MODIFIERS = TokenSet.create(ABSTRACT_KEYWORD, FINAL_KEYWORD, SEALED_KEYWORD, OPEN_KEYWORD);

    TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);

    /**
     * Don't add KDocTokens to COMMENTS TokenSet, because it is used in KotlinParserDefinition.getCommentTokens(),
     * and therefor all COMMENTS tokens will be ignored by PsiBuilder.
     *
     * @see KtPsiUtil#isInComment(com.intellij.psi.PsiElement)
     */
    TokenSet COMMENTS = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT);
    TokenSet WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(COMMENTS, WHITESPACES);

    TokenSet STRINGS = TokenSet.create(CHARACTER_LITERAL, REGULAR_STRING_PART);
    TokenSet OPERATIONS = TokenSet.create(AS_KEYWORD, AS_SAFE, IS_KEYWORD, IN_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, EXCLEXCL, MUL, PLUS,
                                          MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR,
                                          SAFE_ACCESS, ELVIS,
                                          RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,
                                          NOT_IN, NOT_IS,
                                          IDENTIFIER);

    TokenSet AUGMENTED_ASSIGNMENTS = TokenSet.create(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    TokenSet ALL_ASSIGNMENTS = TokenSet.create(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    TokenSet INCREMENT_AND_DECREMENT = TokenSet.create(PLUSPLUS, MINUSMINUS);
    TokenSet QUALIFIED_ACCESS = TokenSet.create(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION);
}
