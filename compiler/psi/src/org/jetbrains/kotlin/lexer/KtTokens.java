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
    int INVALID_Id = 0;
    int EOF_Id = 1;
    int RESERVED_Id = 2;
    int BLOCK_COMMENT_Id = 3;
    int EOL_COMMENT_Id = 4;
    int SHEBANG_COMMENT_Id = 5;
    int INTEGER_LITERAL_Id = 6;
    int FLOAT_LITERAL_Id = 7;
    int CHARACTER_LITERAL_Id = 8;
    int CLOSING_QUOTE_Id = 9;
    int OPEN_QUOTE_Id = 10;
    int REGULAR_STRING_PART_Id = 11;
    int ESCAPE_SEQUENCE_Id = 12;
    int SHORT_TEMPLATE_ENTRY_START_Id = 13;
    int LONG_TEMPLATE_ENTRY_START_Id = 14;
    int LONG_TEMPLATE_ENTRY_END_Id = 15;
    int DANGLING_NEWLINE_Id = 16;
    int PACKAGE_KEYWORD_Id = 17;
    int AS_KEYWORD_Id = 18;
    int TYPE_ALIAS_KEYWORD_Id = 19;
    int CLASS_KEYWORD_Id = 20;
    int THIS_KEYWORD_Id = 21;
    int SUPER_KEYWORD_Id = 22;
    int VAL_KEYWORD_Id = 23;
    int VAR_KEYWORD_Id = 24;
    int FUN_KEYWORD_Id = 25;
    int FOR_KEYWORD_Id = 26;
    int NULL_KEYWORD_Id = 27;
    int TRUE_KEYWORD_Id = 28;
    int FALSE_KEYWORD_Id = 29;
    int IS_KEYWORD_Id = 30;
    int IN_KEYWORD_Id = 31;
    int THROW_KEYWORD_Id = 32;
    int RETURN_KEYWORD_Id = 33;
    int BREAK_KEYWORD_Id = 34;
    int CONTINUE_KEYWORD_Id = 35;
    int OBJECT_KEYWORD_Id = 36;
    int IF_KEYWORD_Id = 37;
    int TRY_KEYWORD_Id = 38;
    int ELSE_KEYWORD_Id = 39;
    int WHILE_KEYWORD_Id = 40;
    int DO_KEYWORD_Id = 41;
    int WHEN_KEYWORD_Id = 42;
    int INTERFACE_KEYWORD_Id = 43;
    int TYPEOF_KEYWORD_Id = 44;
    int AS_SAFE_Id = 45;
    int IDENTIFIER_Id = 46;
    int FIELD_IDENTIFIER_Id = 47;
    int LBRACKET_Id = 48;
    int RBRACKET_Id = 49;
    int LBRACE_Id = 50;
    int RBRACE_Id = 51;
    int LPAR_Id = 52;
    int RPAR_Id = 53;
    int DOT_Id = 54;
    int PLUSPLUS_Id = 55;
    int MINUSMINUS_Id = 56;
    int MUL_Id = 57;
    int PLUS_Id = 58;
    int MINUS_Id = 59;
    int EXCL_Id = 60;
    int DIV_Id = 61;
    int PERC_Id = 62;
    int LT_Id = 63;
    int GT_Id = 64;
    int LTEQ_Id = 65;
    int GTEQ_Id = 66;
    int EQEQEQ_Id = 67;
    int ARROW_Id = 68;
    int DOUBLE_ARROW_Id = 69;
    int EXCLEQEQEQ_Id = 70;
    int EQEQ_Id = 71;
    int EXCLEQ_Id = 72;
    int EXCLEXCL_Id = 73;
    int ANDAND_Id = 74;
    int AND_Id = 75;
    int OROR_Id = 76;
    int SAFE_ACCESS_Id = 77;
    int ELVIS_Id = 78;
    int QUEST_Id = 79;
    int COLONCOLON_Id = 80;
    int COLON_Id = 81;
    int SEMICOLON_Id = 82;
    int DOUBLE_SEMICOLON_Id = 83;
    int RANGE_Id = 84;
    int RANGE_UNTIL_Id = 85;
    int EQ_Id = 86;
    int MULTEQ_Id = 87;
    int DIVEQ_Id = 88;
    int PERCEQ_Id = 89;
    int PLUSEQ_Id = 90;
    int MINUSEQ_Id = 91;
    int NOT_IN_Id = 92;
    int NOT_IS_Id = 93;
    int HASH_Id = 94;
    int AT_Id = 95;
    int COMMA_Id = 96;
    int EOL_OR_SEMICOLON_Id = 97;
    int FILE_KEYWORD_Id = 98;
    int FIELD_KEYWORD_Id = 99;
    int PROPERTY_KEYWORD_Id = 100;
    int RECEIVER_KEYWORD_Id = 101;
    int PARAM_KEYWORD_Id = 102;
    int SETPARAM_KEYWORD_Id = 103;
    int DELEGATE_KEYWORD_Id = 104;
    int IMPORT_KEYWORD_Id = 105;
    int WHERE_KEYWORD_Id = 106;
    int BY_KEYWORD_Id = 107;
    int GET_KEYWORD_Id = 108;
    int SET_KEYWORD_Id = 109;
    int CONSTRUCTOR_KEYWORD_Id = 110;
    int INIT_KEYWORD_Id = 111;
    int CONTEXT_KEYWORD_Id = 112;
    int ABSTRACT_KEYWORD_Id = 113;
    int ENUM_KEYWORD_Id = 114;
    int CONTRACT_KEYWORD_Id = 115;
    int OPEN_KEYWORD_Id = 116;
    int INNER_KEYWORD_Id = 117;
    int OVERRIDE_KEYWORD_Id = 118;
    int PRIVATE_KEYWORD_Id = 119;
    int PUBLIC_KEYWORD_Id = 120;
    int INTERNAL_KEYWORD_Id = 121;
    int PROTECTED_KEYWORD_Id = 122;
    int CATCH_KEYWORD_Id = 123;
    int OUT_KEYWORD_Id = 124;
    int VARARG_KEYWORD_Id = 125;
    int REIFIED_KEYWORD_Id = 126;
    int DYNAMIC_KEYWORD_Id = 127;
    int COMPANION_KEYWORD_Id = 128;
    int SEALED_KEYWORD_Id = 129;
    int FINALLY_KEYWORD_Id = 130;
    int FINAL_KEYWORD_Id = 131;
    int LATEINIT_KEYWORD_Id = 132;
    int DATA_KEYWORD_Id = 133;
    int VALUE_KEYWORD_Id = 134;
    int INLINE_KEYWORD_Id = 135;
    int NOINLINE_KEYWORD_Id = 136;
    int TAILREC_KEYWORD_Id = 137;
    int EXTERNAL_KEYWORD_Id = 138;
    int ANNOTATION_KEYWORD_Id = 139;
    int CROSSINLINE_KEYWORD_Id = 140;
    int OPERATOR_KEYWORD_Id = 141;
    int INFIX_KEYWORD_Id = 142;
    int CONST_KEYWORD_Id = 143;
    int SUSPEND_KEYWORD_Id = 144;
    int EXPECT_KEYWORD_Id = 145;
    int ACTUAL_KEYWORD_Id = 146;
    int INTERPOLATION_PREFIX_Id = 147;
    int ALL_KEYWORD_Id = 148;
    int STATIC_KEYWORD_Id = 149;

    KtToken EOF   = new KtToken("EOF", EOF_Id);

    KtToken RESERVED    = new KtToken("RESERVED", RESERVED_Id);

    KtToken BLOCK_COMMENT     = new KtToken("BLOCK_COMMENT", BLOCK_COMMENT_Id);
    KtToken EOL_COMMENT       = new KtToken("EOL_COMMENT", EOL_COMMENT_Id);
    KtToken SHEBANG_COMMENT   = new KtToken("SHEBANG_COMMENT", SHEBANG_COMMENT_Id);

    IElementType DOC_COMMENT   = KDocTokens.KDOC;

    IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    KtToken INTEGER_LITERAL    = new KtToken("INTEGER_LITERAL", INTEGER_LITERAL_Id);
    KtToken FLOAT_LITERAL      = new KtToken("FLOAT_CONSTANT", FLOAT_LITERAL_Id);
    KtToken CHARACTER_LITERAL  = new KtToken("CHARACTER_LITERAL", CHARACTER_LITERAL_Id);

    KtToken INTERPOLATION_PREFIX = new KtToken("INTERPOLATION_PREFIX", INTERPOLATION_PREFIX_Id);
    KtToken CLOSING_QUOTE = new KtToken("CLOSING_QUOTE", CLOSING_QUOTE_Id);
    KtToken OPEN_QUOTE = new KtToken("OPEN_QUOTE", OPEN_QUOTE_Id);
    KtToken REGULAR_STRING_PART = new KtToken("REGULAR_STRING_PART", REGULAR_STRING_PART_Id);
    KtToken ESCAPE_SEQUENCE = new KtToken("ESCAPE_SEQUENCE", ESCAPE_SEQUENCE_Id);
    KtToken SHORT_TEMPLATE_ENTRY_START = new KtToken("SHORT_TEMPLATE_ENTRY_START", SHORT_TEMPLATE_ENTRY_START_Id);
    KtToken LONG_TEMPLATE_ENTRY_START = new KtToken("LONG_TEMPLATE_ENTRY_START", LONG_TEMPLATE_ENTRY_START_Id);
    KtToken LONG_TEMPLATE_ENTRY_END = new KtToken("LONG_TEMPLATE_ENTRY_END", LONG_TEMPLATE_ENTRY_END_Id);
    KtToken DANGLING_NEWLINE = new KtToken("DANGLING_NEWLINE", DANGLING_NEWLINE_Id);

    KtKeywordToken PACKAGE_KEYWORD          = KtKeywordToken.keyword("package", PACKAGE_KEYWORD_Id);
    KtKeywordToken AS_KEYWORD               = KtKeywordToken.keyword("as", AS_KEYWORD_Id);
    KtKeywordToken TYPE_ALIAS_KEYWORD       = KtKeywordToken.keyword("typealias", TYPE_ALIAS_KEYWORD_Id);
    KtKeywordToken CLASS_KEYWORD            = KtKeywordToken.keyword("class", CLASS_KEYWORD_Id);
    KtKeywordToken THIS_KEYWORD             = KtKeywordToken.keyword("this", THIS_KEYWORD_Id);
    KtKeywordToken SUPER_KEYWORD            = KtKeywordToken.keyword("super", SUPER_KEYWORD_Id);
    KtKeywordToken VAL_KEYWORD              = KtKeywordToken.keyword("val", VAL_KEYWORD_Id);
    KtKeywordToken VAR_KEYWORD              = KtKeywordToken.keyword("var", VAR_KEYWORD_Id);
    KtModifierKeywordToken FUN_KEYWORD      = KtModifierKeywordToken.keywordModifier("fun", FUN_KEYWORD_Id);
    KtKeywordToken FOR_KEYWORD              = KtKeywordToken.keyword("for", FOR_KEYWORD_Id);
    KtKeywordToken NULL_KEYWORD             = KtKeywordToken.keyword("null", NULL_KEYWORD_Id);
    KtKeywordToken TRUE_KEYWORD             = KtKeywordToken.keyword("true", TRUE_KEYWORD_Id);
    KtKeywordToken FALSE_KEYWORD            = KtKeywordToken.keyword("false", FALSE_KEYWORD_Id);
    KtKeywordToken IS_KEYWORD               = KtKeywordToken.keyword("is", IS_KEYWORD_Id);
    KtModifierKeywordToken IN_KEYWORD       = KtModifierKeywordToken.keywordModifier("in", IN_KEYWORD_Id);
    KtKeywordToken THROW_KEYWORD            = KtKeywordToken.keyword("throw", THROW_KEYWORD_Id);
    KtKeywordToken RETURN_KEYWORD           = KtKeywordToken.keyword("return", RETURN_KEYWORD_Id);
    KtKeywordToken BREAK_KEYWORD            = KtKeywordToken.keyword("break", BREAK_KEYWORD_Id);
    KtKeywordToken CONTINUE_KEYWORD         = KtKeywordToken.keyword("continue", CONTINUE_KEYWORD_Id);
    KtKeywordToken OBJECT_KEYWORD           = KtKeywordToken.keyword("object", OBJECT_KEYWORD_Id);
    KtKeywordToken IF_KEYWORD               = KtKeywordToken.keyword("if", IF_KEYWORD_Id);
    KtKeywordToken TRY_KEYWORD              = KtKeywordToken.keyword("try", TRY_KEYWORD_Id);
    KtKeywordToken ELSE_KEYWORD             = KtKeywordToken.keyword("else", ELSE_KEYWORD_Id);
    KtKeywordToken WHILE_KEYWORD            = KtKeywordToken.keyword("while", WHILE_KEYWORD_Id);
    KtKeywordToken DO_KEYWORD               = KtKeywordToken.keyword("do", DO_KEYWORD_Id);
    KtKeywordToken WHEN_KEYWORD             = KtKeywordToken.keyword("when", WHEN_KEYWORD_Id);
    KtKeywordToken INTERFACE_KEYWORD        = KtKeywordToken.keyword("interface", INTERFACE_KEYWORD_Id);

    // Reserved for future use:
    KtKeywordToken TYPEOF_KEYWORD           = KtKeywordToken.keyword("typeof", TYPEOF_KEYWORD_Id);

    KtToken AS_SAFE = KtKeywordToken.keyword("AS_SAFE", AS_SAFE_Id);

    KtToken IDENTIFIER = new KtToken("IDENTIFIER", IDENTIFIER_Id);

    KtToken FIELD_IDENTIFIER = new KtToken("FIELD_IDENTIFIER", FIELD_IDENTIFIER_Id);
    KtSingleValueToken LBRACKET    = new KtSingleValueToken("LBRACKET", "[", LBRACKET_Id);
    KtSingleValueToken RBRACKET    = new KtSingleValueToken("RBRACKET", "]", RBRACKET_Id);
    KtSingleValueToken LBRACE      = new KtSingleValueToken("LBRACE", "{", LBRACE_Id);
    KtSingleValueToken RBRACE      = new KtSingleValueToken("RBRACE", "}", RBRACE_Id);
    KtSingleValueToken LPAR        = new KtSingleValueToken("LPAR", "(", LPAR_Id);
    KtSingleValueToken RPAR        = new KtSingleValueToken("RPAR", ")", RPAR_Id);
    KtSingleValueToken DOT         = new KtSingleValueToken("DOT", ".", DOT_Id);
    KtSingleValueToken PLUSPLUS    = new KtSingleValueToken("PLUSPLUS", "++", PLUSPLUS_Id);
    KtSingleValueToken MINUSMINUS  = new KtSingleValueToken("MINUSMINUS", "--", MINUSMINUS_Id);
    KtSingleValueToken MUL         = new KtSingleValueToken("MUL", "*", MUL_Id);
    KtSingleValueToken PLUS        = new KtSingleValueToken("PLUS", "+", PLUS_Id);
    KtSingleValueToken MINUS       = new KtSingleValueToken("MINUS", "-", MINUS_Id);
    KtSingleValueToken EXCL        = new KtSingleValueToken("EXCL", "!", EXCL_Id);
    KtSingleValueToken DIV         = new KtSingleValueToken("DIV", "/", DIV_Id);
    KtSingleValueToken PERC        = new KtSingleValueToken("PERC", "%", PERC_Id);
    KtSingleValueToken LT          = new KtSingleValueToken("LT", "<", LT_Id);
    KtSingleValueToken GT          = new KtSingleValueToken("GT", ">", GT_Id);
    KtSingleValueToken LTEQ        = new KtSingleValueToken("LTEQ", "<=", LTEQ_Id);
    KtSingleValueToken GTEQ        = new KtSingleValueToken("GTEQ", ">=", GTEQ_Id);
    KtSingleValueToken EQEQEQ      = new KtSingleValueToken("EQEQEQ", "===", EQEQEQ_Id);
    KtSingleValueToken ARROW       = new KtSingleValueToken("ARROW", "->", ARROW_Id);
    KtSingleValueToken DOUBLE_ARROW       = new KtSingleValueToken("DOUBLE_ARROW", "=>", DOUBLE_ARROW_Id);
    KtSingleValueToken EXCLEQEQEQ  = new KtSingleValueToken("EXCLEQEQEQ", "!==", EXCLEQEQEQ_Id);
    KtSingleValueToken EQEQ        = new KtSingleValueToken("EQEQ", "==", EQEQ_Id);
    KtSingleValueToken EXCLEQ      = new KtSingleValueToken("EXCLEQ", "!=", EXCLEQ_Id);
    KtSingleValueToken EXCLEXCL    = new KtSingleValueToken("EXCLEXCL", "!!", EXCLEXCL_Id);
    KtSingleValueToken ANDAND      = new KtSingleValueToken("ANDAND", "&&", ANDAND_Id);
    KtSingleValueToken AND         = new KtSingleValueToken("AND", "&", AND_Id);
    KtSingleValueToken OROR        = new KtSingleValueToken("OROR", "||", OROR_Id);
    KtSingleValueToken SAFE_ACCESS = new KtSingleValueToken("SAFE_ACCESS", "?.", SAFE_ACCESS_Id);
    KtSingleValueToken ELVIS       = new KtSingleValueToken("ELVIS", "?:", ELVIS_Id);
    KtSingleValueToken QUEST       = new KtSingleValueToken("QUEST", "?", QUEST_Id);
    KtSingleValueToken COLONCOLON  = new KtSingleValueToken("COLONCOLON", "::", COLONCOLON_Id);
    KtSingleValueToken COLON       = new KtSingleValueToken("COLON", ":", COLON_Id);
    KtSingleValueToken SEMICOLON   = new KtSingleValueToken("SEMICOLON", ";", SEMICOLON_Id);
    KtSingleValueToken DOUBLE_SEMICOLON   = new KtSingleValueToken("DOUBLE_SEMICOLON", ";;", DOUBLE_SEMICOLON_Id);
    KtSingleValueToken RANGE       = new KtSingleValueToken("RANGE", "..", RANGE_Id);
    KtSingleValueToken RANGE_UNTIL       = new KtSingleValueToken("RANGE_UNTIL", "..<", RANGE_UNTIL_Id);
    KtSingleValueToken EQ          = new KtSingleValueToken("EQ", "=", EQ_Id);
    KtSingleValueToken MULTEQ      = new KtSingleValueToken("MULTEQ", "*=", MULTEQ_Id);
    KtSingleValueToken DIVEQ       = new KtSingleValueToken("DIVEQ", "/=", DIVEQ_Id);
    KtSingleValueToken PERCEQ      = new KtSingleValueToken("PERCEQ", "%=", PERCEQ_Id);
    KtSingleValueToken PLUSEQ      = new KtSingleValueToken("PLUSEQ", "+=", PLUSEQ_Id);
    KtSingleValueToken MINUSEQ     = new KtSingleValueToken("MINUSEQ", "-=", MINUSEQ_Id);
    KtKeywordToken NOT_IN      = KtKeywordToken.keyword("NOT_IN", "!in", NOT_IN_Id);
    KtKeywordToken NOT_IS      = KtKeywordToken.keyword("NOT_IS", "!is", NOT_IS_Id);
    KtSingleValueToken HASH        = new KtSingleValueToken("HASH", "#", HASH_Id);
    KtSingleValueToken AT          = new KtSingleValueToken("AT", "@", AT_Id);

    KtSingleValueToken COMMA       = new KtSingleValueToken("COMMA", ",", COMMA_Id);

    KtToken EOL_OR_SEMICOLON   = new KtToken("EOL_OR_SEMICOLON", EOL_OR_SEMICOLON_Id);
    KtKeywordToken ALL_KEYWORD    = KtKeywordToken.softKeyword("all", ALL_KEYWORD_Id);
    KtKeywordToken FILE_KEYWORD    = KtKeywordToken.softKeyword("file", FILE_KEYWORD_Id);
    KtKeywordToken FIELD_KEYWORD     = KtKeywordToken.softKeyword("field", FIELD_KEYWORD_Id);
    KtKeywordToken PROPERTY_KEYWORD     = KtKeywordToken.softKeyword("property", PROPERTY_KEYWORD_Id);
    KtKeywordToken RECEIVER_KEYWORD     = KtKeywordToken.softKeyword("receiver", RECEIVER_KEYWORD_Id);
    KtKeywordToken PARAM_KEYWORD     = KtKeywordToken.softKeyword("param", PARAM_KEYWORD_Id);
    KtKeywordToken SETPARAM_KEYWORD  = KtKeywordToken.softKeyword("setparam", SETPARAM_KEYWORD_Id);
    KtKeywordToken DELEGATE_KEYWORD  = KtKeywordToken.softKeyword("delegate", DELEGATE_KEYWORD_Id);
    KtKeywordToken IMPORT_KEYWORD    = KtKeywordToken.softKeyword("import", IMPORT_KEYWORD_Id);
    KtKeywordToken WHERE_KEYWORD     = KtKeywordToken.softKeyword("where", WHERE_KEYWORD_Id);
    KtKeywordToken BY_KEYWORD        = KtKeywordToken.softKeyword("by", BY_KEYWORD_Id);
    KtKeywordToken GET_KEYWORD       = KtKeywordToken.softKeyword("get", GET_KEYWORD_Id);
    KtKeywordToken SET_KEYWORD       = KtKeywordToken.softKeyword("set", SET_KEYWORD_Id);
    KtKeywordToken CONSTRUCTOR_KEYWORD = KtKeywordToken.softKeyword("constructor", CONSTRUCTOR_KEYWORD_Id);
    KtKeywordToken INIT_KEYWORD        = KtKeywordToken.softKeyword("init", INIT_KEYWORD_Id);
    KtKeywordToken CONTEXT_KEYWORD     = KtKeywordToken.softKeyword("context", CONTEXT_KEYWORD_Id);

    KtModifierKeywordToken ABSTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("abstract", ABSTRACT_KEYWORD_Id);
    KtModifierKeywordToken ENUM_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("enum", ENUM_KEYWORD_Id);
    KtModifierKeywordToken CONTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("contract", CONTRACT_KEYWORD_Id);
    KtModifierKeywordToken OPEN_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("open", OPEN_KEYWORD_Id);
    KtModifierKeywordToken INNER_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("inner", INNER_KEYWORD_Id);
    KtModifierKeywordToken OVERRIDE_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("override", OVERRIDE_KEYWORD_Id);
    KtModifierKeywordToken PRIVATE_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("private", PRIVATE_KEYWORD_Id);
    KtModifierKeywordToken PUBLIC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("public", PUBLIC_KEYWORD_Id);
    KtModifierKeywordToken INTERNAL_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("internal", INTERNAL_KEYWORD_Id);
    KtModifierKeywordToken PROTECTED_KEYWORD = KtModifierKeywordToken.softKeywordModifier("protected", PROTECTED_KEYWORD_Id);
    KtKeywordToken CATCH_KEYWORD     = KtKeywordToken.softKeyword("catch", CATCH_KEYWORD_Id);
    KtModifierKeywordToken OUT_KEYWORD       = KtModifierKeywordToken.softKeywordModifier("out", OUT_KEYWORD_Id);
    KtModifierKeywordToken VARARG_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("vararg", VARARG_KEYWORD_Id);
    KtModifierKeywordToken REIFIED_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("reified", REIFIED_KEYWORD_Id);
    KtKeywordToken DYNAMIC_KEYWORD   = KtKeywordToken.softKeyword("dynamic", DYNAMIC_KEYWORD_Id);
    KtModifierKeywordToken COMPANION_KEYWORD = KtModifierKeywordToken.softKeywordModifier("companion", COMPANION_KEYWORD_Id);
    KtModifierKeywordToken SEALED_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("sealed", SEALED_KEYWORD_Id);
    KtModifierKeywordToken STATIC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("static", STATIC_KEYWORD_Id);

    KtModifierKeywordToken DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD;

    KtKeywordToken FINALLY_KEYWORD   = KtKeywordToken.softKeyword("finally", FINALLY_KEYWORD_Id);
    KtModifierKeywordToken FINAL_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("final", FINAL_KEYWORD_Id);

    KtModifierKeywordToken LATEINIT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("lateinit", LATEINIT_KEYWORD_Id);

    KtModifierKeywordToken DATA_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("data", DATA_KEYWORD_Id);
    KtModifierKeywordToken VALUE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("value", VALUE_KEYWORD_Id);
    KtModifierKeywordToken INLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("inline", INLINE_KEYWORD_Id);
    KtModifierKeywordToken NOINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("noinline", NOINLINE_KEYWORD_Id);
    KtModifierKeywordToken TAILREC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("tailrec", TAILREC_KEYWORD_Id);
    KtModifierKeywordToken EXTERNAL_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("external", EXTERNAL_KEYWORD_Id);
    KtModifierKeywordToken ANNOTATION_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("annotation", ANNOTATION_KEYWORD_Id);
    KtModifierKeywordToken CROSSINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("crossinline", CROSSINLINE_KEYWORD_Id);
    KtModifierKeywordToken OPERATOR_KEYWORD = KtModifierKeywordToken.softKeywordModifier("operator", OPERATOR_KEYWORD_Id);
    KtModifierKeywordToken INFIX_KEYWORD = KtModifierKeywordToken.softKeywordModifier("infix", INFIX_KEYWORD_Id);

    KtModifierKeywordToken CONST_KEYWORD = KtModifierKeywordToken.softKeywordModifier("const", CONST_KEYWORD_Id);

    KtModifierKeywordToken SUSPEND_KEYWORD = KtModifierKeywordToken.softKeywordModifier("suspend", SUSPEND_KEYWORD_Id);

    KtModifierKeywordToken EXPECT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("expect", EXPECT_KEYWORD_Id);
    KtModifierKeywordToken ACTUAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("actual", ACTUAL_KEYWORD_Id);


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
                                             DELEGATE_KEYWORD, ALL_KEYWORD,
                                             LATEINIT_KEYWORD,
                                             DATA_KEYWORD, INLINE_KEYWORD, NOINLINE_KEYWORD, TAILREC_KEYWORD, EXTERNAL_KEYWORD,
                                             ANNOTATION_KEYWORD, CROSSINLINE_KEYWORD, CONST_KEYWORD, OPERATOR_KEYWORD, INFIX_KEYWORD,
                                             SUSPEND_KEYWORD, EXPECT_KEYWORD, ACTUAL_KEYWORD,
                                             VALUE_KEYWORD, CONTEXT_KEYWORD, STATIC_KEYWORD
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
                    EXPECT_KEYWORD, ACTUAL_KEYWORD, FUN_KEYWORD, VALUE_KEYWORD, STATIC_KEYWORD
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
                                          RANGE, RANGE_UNTIL, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,
                                          NOT_IN, NOT_IS,
                                          IDENTIFIER);

    TokenSet AUGMENTED_ASSIGNMENTS = TokenSet.create(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    TokenSet ALL_ASSIGNMENTS = TokenSet.create(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    TokenSet INCREMENT_AND_DECREMENT = TokenSet.create(PLUSPLUS, MINUSMINUS);
    TokenSet QUALIFIED_ACCESS = TokenSet.create(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION);
}
