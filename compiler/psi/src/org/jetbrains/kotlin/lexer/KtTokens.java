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
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes;
import org.jetbrains.kotlin.psi.KtPsiUtil;

@SuppressWarnings("WeakerAccess") // Let all static identifiers be public as well as corresponding elements
public class KtTokens {
    static {
        // It forces initializing tokens in strict order that provides possibility to match indexes and static identifiers
        @SuppressWarnings("unused")
        IElementType dependentTokensInit = KDocElementTypes.KDOC_SECTION;
    }

    public static final int INVALID_Id = 0;
    public static final int EOF_Id = 1;
    public static final int RESERVED_Id = 2;
    public static final int BLOCK_COMMENT_Id = 3;
    public static final int EOL_COMMENT_Id = 4;
    public static final int SHEBANG_COMMENT_Id = 5;
    public static final int INTEGER_LITERAL_Id = 6;
    public static final int FLOAT_LITERAL_Id = 7;
    public static final int CHARACTER_LITERAL_Id = 8;
    public static final int CLOSING_QUOTE_Id = 9;
    public static final int OPEN_QUOTE_Id = 10;
    public static final int REGULAR_STRING_PART_Id = 11;
    public static final int ESCAPE_SEQUENCE_Id = 12;
    public static final int SHORT_TEMPLATE_ENTRY_START_Id = 13;
    public static final int LONG_TEMPLATE_ENTRY_START_Id = 14;
    public static final int LONG_TEMPLATE_ENTRY_END_Id = 15;
    public static final int DANGLING_NEWLINE_Id = 16;
    public static final int PACKAGE_KEYWORD_Id = 17;
    public static final int AS_KEYWORD_Id = 18;
    public static final int TYPE_ALIAS_KEYWORD_Id = 19;
    public static final int CLASS_KEYWORD_Id = 20;
    public static final int THIS_KEYWORD_Id = 21;
    public static final int SUPER_KEYWORD_Id = 22;
    public static final int VAL_KEYWORD_Id = 23;
    public static final int VAR_KEYWORD_Id = 24;
    public static final int FUN_KEYWORD_Id = 25;
    public static final int FOR_KEYWORD_Id = 26;
    public static final int NULL_KEYWORD_Id = 27;
    public static final int TRUE_KEYWORD_Id = 28;
    public static final int FALSE_KEYWORD_Id = 29;
    public static final int IS_KEYWORD_Id = 30;
    public static final int IN_KEYWORD_Id = 31;
    public static final int THROW_KEYWORD_Id = 32;
    public static final int RETURN_KEYWORD_Id = 33;
    public static final int BREAK_KEYWORD_Id = 34;
    public static final int CONTINUE_KEYWORD_Id = 35;
    public static final int OBJECT_KEYWORD_Id = 36;
    public static final int IF_KEYWORD_Id = 37;
    public static final int TRY_KEYWORD_Id = 38;
    public static final int ELSE_KEYWORD_Id = 39;
    public static final int WHILE_KEYWORD_Id = 40;
    public static final int DO_KEYWORD_Id = 41;
    public static final int WHEN_KEYWORD_Id = 42;
    public static final int INTERFACE_KEYWORD_Id = 43;
    public static final int TYPEOF_KEYWORD_Id = 44;
    public static final int AS_SAFE_Id = 45;
    public static final int IDENTIFIER_Id = 46;
    public static final int FIELD_IDENTIFIER_Id = 47;
    public static final int LBRACKET_Id = 48;
    public static final int RBRACKET_Id = 49;
    public static final int LBRACE_Id = 50;
    public static final int RBRACE_Id = 51;
    public static final int LPAR_Id = 52;
    public static final int RPAR_Id = 53;
    public static final int DOT_Id = 54;
    public static final int PLUSPLUS_Id = 55;
    public static final int MINUSMINUS_Id = 56;
    public static final int MUL_Id = 57;
    public static final int PLUS_Id = 58;
    public static final int MINUS_Id = 59;
    public static final int EXCL_Id = 60;
    public static final int DIV_Id = 61;
    public static final int PERC_Id = 62;
    public static final int LT_Id = 63;
    public static final int GT_Id = 64;
    public static final int LTEQ_Id = 65;
    public static final int GTEQ_Id = 66;
    public static final int EQEQEQ_Id = 67;
    public static final int ARROW_Id = 68;
    public static final int DOUBLE_ARROW_Id = 69;
    public static final int EXCLEQEQEQ_Id = 70;
    public static final int EQEQ_Id = 71;
    public static final int EXCLEQ_Id = 72;
    public static final int EXCLEXCL_Id = 73;
    public static final int ANDAND_Id = 74;
    public static final int AND_Id = 75;
    public static final int OROR_Id = 76;
    public static final int SAFE_ACCESS_Id = 77;
    public static final int ELVIS_Id = 78;
    public static final int QUEST_Id = 79;
    public static final int COLONCOLON_Id = 80;
    public static final int COLON_Id = 81;
    public static final int SEMICOLON_Id = 82;
    public static final int DOUBLE_SEMICOLON_Id = 83;
    public static final int RANGE_Id = 84;
    public static final int RANGE_UNTIL_Id = 85;
    public static final int EQ_Id = 86;
    public static final int MULTEQ_Id = 87;
    public static final int DIVEQ_Id = 88;
    public static final int PERCEQ_Id = 89;
    public static final int PLUSEQ_Id = 90;
    public static final int MINUSEQ_Id = 91;
    public static final int NOT_IN_Id = 92;
    public static final int NOT_IS_Id = 93;
    public static final int HASH_Id = 94;
    public static final int AT_Id = 95;
    public static final int COMMA_Id = 96;
    public static final int EOL_OR_SEMICOLON_Id = 97;
    public static final int FILE_KEYWORD_Id = 98;
    public static final int FIELD_KEYWORD_Id = 99;
    public static final int PROPERTY_KEYWORD_Id = 100;
    public static final int RECEIVER_KEYWORD_Id = 101;
    public static final int PARAM_KEYWORD_Id = 102;
    public static final int SETPARAM_KEYWORD_Id = 103;
    public static final int DELEGATE_KEYWORD_Id = 104;
    public static final int IMPORT_KEYWORD_Id = 105;
    public static final int WHERE_KEYWORD_Id = 106;
    public static final int BY_KEYWORD_Id = 107;
    public static final int GET_KEYWORD_Id = 108;
    public static final int SET_KEYWORD_Id = 109;
    public static final int CONSTRUCTOR_KEYWORD_Id = 110;
    public static final int INIT_KEYWORD_Id = 111;
    public static final int CONTEXT_KEYWORD_Id = 112;
    public static final int ABSTRACT_KEYWORD_Id = 113;
    public static final int ENUM_KEYWORD_Id = 114;
    public static final int CONTRACT_KEYWORD_Id = 115;
    public static final int OPEN_KEYWORD_Id = 116;
    public static final int INNER_KEYWORD_Id = 117;
    public static final int OVERRIDE_KEYWORD_Id = 118;
    public static final int PRIVATE_KEYWORD_Id = 119;
    public static final int PUBLIC_KEYWORD_Id = 120;
    public static final int INTERNAL_KEYWORD_Id = 121;
    public static final int PROTECTED_KEYWORD_Id = 122;
    public static final int CATCH_KEYWORD_Id = 123;
    public static final int OUT_KEYWORD_Id = 124;
    public static final int VARARG_KEYWORD_Id = 125;
    public static final int REIFIED_KEYWORD_Id = 126;
    public static final int DYNAMIC_KEYWORD_Id = 127;
    public static final int COMPANION_KEYWORD_Id = 128;
    public static final int SEALED_KEYWORD_Id = 129;
    public static final int FINALLY_KEYWORD_Id = 130;
    public static final int FINAL_KEYWORD_Id = 131;
    public static final int LATEINIT_KEYWORD_Id = 132;
    public static final int DATA_KEYWORD_Id = 133;
    public static final int VALUE_KEYWORD_Id = 134;
    public static final int INLINE_KEYWORD_Id = 135;
    public static final int NOINLINE_KEYWORD_Id = 136;
    public static final int TAILREC_KEYWORD_Id = 137;
    public static final int EXTERNAL_KEYWORD_Id = 138;
    public static final int ANNOTATION_KEYWORD_Id = 139;
    public static final int CROSSINLINE_KEYWORD_Id = 140;
    public static final int OPERATOR_KEYWORD_Id = 141;
    public static final int INFIX_KEYWORD_Id = 142;
    public static final int CONST_KEYWORD_Id = 143;
    public static final int SUSPEND_KEYWORD_Id = 144;
    public static final int EXPECT_KEYWORD_Id = 145;
    public static final int ACTUAL_KEYWORD_Id = 146;
    public static final int INTERPOLATION_PREFIX_Id = 147;
    public static final int ALL_KEYWORD_Id = 148;

    public static final KtToken EOF   = new KtToken("EOF", EOF_Id);

    public static final KtToken RESERVED    = new KtToken("RESERVED", RESERVED_Id);

    public static final KtToken BLOCK_COMMENT     = new KtToken("BLOCK_COMMENT", BLOCK_COMMENT_Id);
    public static final KtToken EOL_COMMENT       = new KtToken("EOL_COMMENT", EOL_COMMENT_Id);
    public static final KtToken SHEBANG_COMMENT   = new KtToken("SHEBANG_COMMENT", SHEBANG_COMMENT_Id);

    public static final IElementType DOC_COMMENT   = KDocTokens.KDOC;

    public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    public static final KtToken INTEGER_LITERAL    = new KtToken("INTEGER_LITERAL", INTEGER_LITERAL_Id);
    public static final KtToken FLOAT_LITERAL      = new KtToken("FLOAT_CONSTANT", FLOAT_LITERAL_Id);
    public static final KtToken CHARACTER_LITERAL  = new KtToken("CHARACTER_LITERAL", CHARACTER_LITERAL_Id);

    public static final KtToken INTERPOLATION_PREFIX = new KtToken("INTERPOLATION_PREFIX", INTERPOLATION_PREFIX_Id);
    public static final KtToken CLOSING_QUOTE = new KtToken("CLOSING_QUOTE", CLOSING_QUOTE_Id);
    public static final KtToken OPEN_QUOTE = new KtToken("OPEN_QUOTE", OPEN_QUOTE_Id);
    public static final KtToken REGULAR_STRING_PART = new KtToken("REGULAR_STRING_PART", REGULAR_STRING_PART_Id);
    public static final KtToken ESCAPE_SEQUENCE = new KtToken("ESCAPE_SEQUENCE", ESCAPE_SEQUENCE_Id);
    public static final KtToken SHORT_TEMPLATE_ENTRY_START = new KtToken("SHORT_TEMPLATE_ENTRY_START", SHORT_TEMPLATE_ENTRY_START_Id);
    public static final KtToken LONG_TEMPLATE_ENTRY_START = new KtToken("LONG_TEMPLATE_ENTRY_START", LONG_TEMPLATE_ENTRY_START_Id);
    public static final KtToken LONG_TEMPLATE_ENTRY_END = new KtToken("LONG_TEMPLATE_ENTRY_END", LONG_TEMPLATE_ENTRY_END_Id);
    public static final KtToken DANGLING_NEWLINE = new KtToken("DANGLING_NEWLINE", DANGLING_NEWLINE_Id);

    public static final KtKeywordToken PACKAGE_KEYWORD          = KtKeywordToken.keyword("package", PACKAGE_KEYWORD_Id);
    public static final KtKeywordToken AS_KEYWORD               = KtKeywordToken.keyword("as", AS_KEYWORD_Id);
    public static final KtKeywordToken TYPE_ALIAS_KEYWORD       = KtKeywordToken.keyword("typealias", TYPE_ALIAS_KEYWORD_Id);
    public static final KtKeywordToken CLASS_KEYWORD            = KtKeywordToken.keyword("class", CLASS_KEYWORD_Id);
    public static final KtKeywordToken THIS_KEYWORD             = KtKeywordToken.keyword("this", THIS_KEYWORD_Id);
    public static final KtKeywordToken SUPER_KEYWORD            = KtKeywordToken.keyword("super", SUPER_KEYWORD_Id);
    public static final KtKeywordToken VAL_KEYWORD              = KtKeywordToken.keyword("val", VAL_KEYWORD_Id);
    public static final KtKeywordToken VAR_KEYWORD              = KtKeywordToken.keyword("var", VAR_KEYWORD_Id);
    public static final KtModifierKeywordToken FUN_KEYWORD      = KtModifierKeywordToken.keywordModifier("fun", FUN_KEYWORD_Id);
    public static final KtKeywordToken FOR_KEYWORD              = KtKeywordToken.keyword("for", FOR_KEYWORD_Id);
    public static final KtKeywordToken NULL_KEYWORD             = KtKeywordToken.keyword("null", NULL_KEYWORD_Id);
    public static final KtKeywordToken TRUE_KEYWORD             = KtKeywordToken.keyword("true", TRUE_KEYWORD_Id);
    public static final KtKeywordToken FALSE_KEYWORD            = KtKeywordToken.keyword("false", FALSE_KEYWORD_Id);
    public static final KtKeywordToken IS_KEYWORD               = KtKeywordToken.keyword("is", IS_KEYWORD_Id);
    public static final KtModifierKeywordToken IN_KEYWORD       = KtModifierKeywordToken.keywordModifier("in", IN_KEYWORD_Id);
    public static final KtKeywordToken THROW_KEYWORD            = KtKeywordToken.keyword("throw", THROW_KEYWORD_Id);
    public static final KtKeywordToken RETURN_KEYWORD           = KtKeywordToken.keyword("return", RETURN_KEYWORD_Id);
    public static final KtKeywordToken BREAK_KEYWORD            = KtKeywordToken.keyword("break", BREAK_KEYWORD_Id);
    public static final KtKeywordToken CONTINUE_KEYWORD         = KtKeywordToken.keyword("continue", CONTINUE_KEYWORD_Id);
    public static final KtKeywordToken OBJECT_KEYWORD           = KtKeywordToken.keyword("object", OBJECT_KEYWORD_Id);
    public static final KtKeywordToken IF_KEYWORD               = KtKeywordToken.keyword("if", IF_KEYWORD_Id);
    public static final KtKeywordToken TRY_KEYWORD              = KtKeywordToken.keyword("try", TRY_KEYWORD_Id);
    public static final KtKeywordToken ELSE_KEYWORD             = KtKeywordToken.keyword("else", ELSE_KEYWORD_Id);
    public static final KtKeywordToken WHILE_KEYWORD            = KtKeywordToken.keyword("while", WHILE_KEYWORD_Id);
    public static final KtKeywordToken DO_KEYWORD               = KtKeywordToken.keyword("do", DO_KEYWORD_Id);
    public static final KtKeywordToken WHEN_KEYWORD             = KtKeywordToken.keyword("when", WHEN_KEYWORD_Id);
    public static final KtKeywordToken INTERFACE_KEYWORD        = KtKeywordToken.keyword("interface", INTERFACE_KEYWORD_Id);

    // Reserved for future use:
    public static final KtKeywordToken TYPEOF_KEYWORD           = KtKeywordToken.keyword("typeof", TYPEOF_KEYWORD_Id);

    public static final KtToken AS_SAFE = KtKeywordToken.keyword("AS_SAFE", AS_SAFE_Id);

    public static final KtToken IDENTIFIER = new KtToken("IDENTIFIER", IDENTIFIER_Id);

    public static final KtToken FIELD_IDENTIFIER = new KtToken("FIELD_IDENTIFIER", FIELD_IDENTIFIER_Id);
    public static final KtSingleValueToken LBRACKET    = new KtSingleValueToken("LBRACKET", "[", LBRACKET_Id);
    public static final KtSingleValueToken RBRACKET    = new KtSingleValueToken("RBRACKET", "]", RBRACKET_Id);
    public static final KtSingleValueToken LBRACE      = new KtSingleValueToken("LBRACE", "{", LBRACE_Id);
    public static final KtSingleValueToken RBRACE      = new KtSingleValueToken("RBRACE", "}", RBRACE_Id);
    public static final KtSingleValueToken LPAR        = new KtSingleValueToken("LPAR", "(", LPAR_Id);
    public static final KtSingleValueToken RPAR        = new KtSingleValueToken("RPAR", ")", RPAR_Id);
    public static final KtSingleValueToken DOT         = new KtSingleValueToken("DOT", ".", DOT_Id);
    public static final KtSingleValueToken PLUSPLUS    = new KtSingleValueToken("PLUSPLUS", "++", PLUSPLUS_Id);
    public static final KtSingleValueToken MINUSMINUS  = new KtSingleValueToken("MINUSMINUS", "--", MINUSMINUS_Id);
    public static final KtSingleValueToken MUL         = new KtSingleValueToken("MUL", "*", MUL_Id);
    public static final KtSingleValueToken PLUS        = new KtSingleValueToken("PLUS", "+", PLUS_Id);
    public static final KtSingleValueToken MINUS       = new KtSingleValueToken("MINUS", "-", MINUS_Id);
    public static final KtSingleValueToken EXCL        = new KtSingleValueToken("EXCL", "!", EXCL_Id);
    public static final KtSingleValueToken DIV         = new KtSingleValueToken("DIV", "/", DIV_Id);
    public static final KtSingleValueToken PERC        = new KtSingleValueToken("PERC", "%", PERC_Id);
    public static final KtSingleValueToken LT          = new KtSingleValueToken("LT", "<", LT_Id);
    public static final KtSingleValueToken GT          = new KtSingleValueToken("GT", ">", GT_Id);
    public static final KtSingleValueToken LTEQ        = new KtSingleValueToken("LTEQ", "<=", LTEQ_Id);
    public static final KtSingleValueToken GTEQ        = new KtSingleValueToken("GTEQ", ">=", GTEQ_Id);
    public static final KtSingleValueToken EQEQEQ      = new KtSingleValueToken("EQEQEQ", "===", EQEQEQ_Id);
    public static final KtSingleValueToken ARROW       = new KtSingleValueToken("ARROW", "->", ARROW_Id);
    public static final KtSingleValueToken DOUBLE_ARROW       = new KtSingleValueToken("DOUBLE_ARROW", "=>", DOUBLE_ARROW_Id);
    public static final KtSingleValueToken EXCLEQEQEQ  = new KtSingleValueToken("EXCLEQEQEQ", "!==", EXCLEQEQEQ_Id);
    public static final KtSingleValueToken EQEQ        = new KtSingleValueToken("EQEQ", "==", EQEQ_Id);
    public static final KtSingleValueToken EXCLEQ      = new KtSingleValueToken("EXCLEQ", "!=", EXCLEQ_Id);
    public static final KtSingleValueToken EXCLEXCL    = new KtSingleValueToken("EXCLEXCL", "!!", EXCLEXCL_Id);
    public static final KtSingleValueToken ANDAND      = new KtSingleValueToken("ANDAND", "&&", ANDAND_Id);
    public static final KtSingleValueToken AND         = new KtSingleValueToken("AND", "&", AND_Id);
    public static final KtSingleValueToken OROR        = new KtSingleValueToken("OROR", "||", OROR_Id);
    public static final KtSingleValueToken SAFE_ACCESS = new KtSingleValueToken("SAFE_ACCESS", "?.", SAFE_ACCESS_Id);
    public static final KtSingleValueToken ELVIS       = new KtSingleValueToken("ELVIS", "?:", ELVIS_Id);
    public static final KtSingleValueToken QUEST       = new KtSingleValueToken("QUEST", "?", QUEST_Id);
    public static final KtSingleValueToken COLONCOLON  = new KtSingleValueToken("COLONCOLON", "::", COLONCOLON_Id);
    public static final KtSingleValueToken COLON       = new KtSingleValueToken("COLON", ":", COLON_Id);
    public static final KtSingleValueToken SEMICOLON   = new KtSingleValueToken("SEMICOLON", ";", SEMICOLON_Id);
    public static final KtSingleValueToken DOUBLE_SEMICOLON   = new KtSingleValueToken("DOUBLE_SEMICOLON", ";;", DOUBLE_SEMICOLON_Id);
    public static final KtSingleValueToken RANGE       = new KtSingleValueToken("RANGE", "..", RANGE_Id);
    public static final KtSingleValueToken RANGE_UNTIL       = new KtSingleValueToken("RANGE_UNTIL", "..<", RANGE_UNTIL_Id);
    public static final KtSingleValueToken EQ          = new KtSingleValueToken("EQ", "=", EQ_Id);
    public static final KtSingleValueToken MULTEQ      = new KtSingleValueToken("MULTEQ", "*=", MULTEQ_Id);
    public static final KtSingleValueToken DIVEQ       = new KtSingleValueToken("DIVEQ", "/=", DIVEQ_Id);
    public static final KtSingleValueToken PERCEQ      = new KtSingleValueToken("PERCEQ", "%=", PERCEQ_Id);
    public static final KtSingleValueToken PLUSEQ      = new KtSingleValueToken("PLUSEQ", "+=", PLUSEQ_Id);
    public static final KtSingleValueToken MINUSEQ     = new KtSingleValueToken("MINUSEQ", "-=", MINUSEQ_Id);
    public static final KtKeywordToken NOT_IN      = KtKeywordToken.keyword("NOT_IN", "!in", NOT_IN_Id);
    public static final KtKeywordToken NOT_IS      = KtKeywordToken.keyword("NOT_IS", "!is", NOT_IS_Id);
    public static final KtSingleValueToken HASH        = new KtSingleValueToken("HASH", "#", HASH_Id);
    public static final KtSingleValueToken AT          = new KtSingleValueToken("AT", "@", AT_Id);

    public static final KtSingleValueToken COMMA       = new KtSingleValueToken("COMMA", ",", COMMA_Id);

    public static final KtToken EOL_OR_SEMICOLON   = new KtToken("EOL_OR_SEMICOLON", EOL_OR_SEMICOLON_Id);
    public static final KtKeywordToken ALL_KEYWORD    = KtKeywordToken.softKeyword("all", ALL_KEYWORD_Id);
    public static final KtKeywordToken FILE_KEYWORD    = KtKeywordToken.softKeyword("file", FILE_KEYWORD_Id);
    public static final KtKeywordToken FIELD_KEYWORD     = KtKeywordToken.softKeyword("field", FIELD_KEYWORD_Id);
    public static final KtKeywordToken PROPERTY_KEYWORD     = KtKeywordToken.softKeyword("property", PROPERTY_KEYWORD_Id);
    public static final KtKeywordToken RECEIVER_KEYWORD     = KtKeywordToken.softKeyword("receiver", RECEIVER_KEYWORD_Id);
    public static final KtKeywordToken PARAM_KEYWORD     = KtKeywordToken.softKeyword("param", PARAM_KEYWORD_Id);
    public static final KtKeywordToken SETPARAM_KEYWORD  = KtKeywordToken.softKeyword("setparam", SETPARAM_KEYWORD_Id);
    public static final KtKeywordToken DELEGATE_KEYWORD  = KtKeywordToken.softKeyword("delegate", DELEGATE_KEYWORD_Id);
    public static final KtKeywordToken IMPORT_KEYWORD    = KtKeywordToken.softKeyword("import", IMPORT_KEYWORD_Id);
    public static final KtKeywordToken WHERE_KEYWORD     = KtKeywordToken.softKeyword("where", WHERE_KEYWORD_Id);
    public static final KtKeywordToken BY_KEYWORD        = KtKeywordToken.softKeyword("by", BY_KEYWORD_Id);
    public static final KtKeywordToken GET_KEYWORD       = KtKeywordToken.softKeyword("get", GET_KEYWORD_Id);
    public static final KtKeywordToken SET_KEYWORD       = KtKeywordToken.softKeyword("set", SET_KEYWORD_Id);
    public static final KtKeywordToken CONSTRUCTOR_KEYWORD = KtKeywordToken.softKeyword("constructor", CONSTRUCTOR_KEYWORD_Id);
    public static final KtKeywordToken INIT_KEYWORD        = KtKeywordToken.softKeyword("init", INIT_KEYWORD_Id);
    public static final KtKeywordToken CONTEXT_KEYWORD     = KtKeywordToken.softKeyword("context", CONTEXT_KEYWORD_Id);

    public static final KtModifierKeywordToken ABSTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("abstract", ABSTRACT_KEYWORD_Id);
    public static final KtModifierKeywordToken ENUM_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("enum", ENUM_KEYWORD_Id);
    public static final KtModifierKeywordToken CONTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("contract", CONTRACT_KEYWORD_Id);
    public static final KtModifierKeywordToken OPEN_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("open", OPEN_KEYWORD_Id);
    public static final KtModifierKeywordToken INNER_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("inner", INNER_KEYWORD_Id);
    public static final KtModifierKeywordToken OVERRIDE_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("override", OVERRIDE_KEYWORD_Id);
    public static final KtModifierKeywordToken PRIVATE_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("private", PRIVATE_KEYWORD_Id);
    public static final KtModifierKeywordToken PUBLIC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("public", PUBLIC_KEYWORD_Id);
    public static final KtModifierKeywordToken INTERNAL_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("internal", INTERNAL_KEYWORD_Id);
    public static final KtModifierKeywordToken PROTECTED_KEYWORD = KtModifierKeywordToken.softKeywordModifier("protected", PROTECTED_KEYWORD_Id);
    public static final KtKeywordToken CATCH_KEYWORD     = KtKeywordToken.softKeyword("catch", CATCH_KEYWORD_Id);
    public static final KtModifierKeywordToken OUT_KEYWORD       = KtModifierKeywordToken.softKeywordModifier("out", OUT_KEYWORD_Id);
    public static final KtModifierKeywordToken VARARG_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("vararg", VARARG_KEYWORD_Id);
    public static final KtModifierKeywordToken REIFIED_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("reified", REIFIED_KEYWORD_Id);
    public static final KtKeywordToken DYNAMIC_KEYWORD   = KtKeywordToken.softKeyword("dynamic", DYNAMIC_KEYWORD_Id);
    public static final KtModifierKeywordToken COMPANION_KEYWORD = KtModifierKeywordToken.softKeywordModifier("companion", COMPANION_KEYWORD_Id);
    public static final KtModifierKeywordToken SEALED_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("sealed", SEALED_KEYWORD_Id);

    public static final KtModifierKeywordToken DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD;

    public static final KtKeywordToken FINALLY_KEYWORD   = KtKeywordToken.softKeyword("finally", FINALLY_KEYWORD_Id);
    public static final KtModifierKeywordToken FINAL_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("final", FINAL_KEYWORD_Id);

    public static final KtModifierKeywordToken LATEINIT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("lateinit", LATEINIT_KEYWORD_Id);

    public static final KtModifierKeywordToken DATA_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("data", DATA_KEYWORD_Id);
    public static final KtModifierKeywordToken VALUE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("value", VALUE_KEYWORD_Id);
    public static final KtModifierKeywordToken INLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("inline", INLINE_KEYWORD_Id);
    public static final KtModifierKeywordToken NOINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("noinline", NOINLINE_KEYWORD_Id);
    public static final KtModifierKeywordToken TAILREC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("tailrec", TAILREC_KEYWORD_Id);
    public static final KtModifierKeywordToken EXTERNAL_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("external", EXTERNAL_KEYWORD_Id);
    public static final KtModifierKeywordToken ANNOTATION_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("annotation", ANNOTATION_KEYWORD_Id);
    public static final KtModifierKeywordToken CROSSINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("crossinline", CROSSINLINE_KEYWORD_Id);
    public static final KtModifierKeywordToken OPERATOR_KEYWORD = KtModifierKeywordToken.softKeywordModifier("operator", OPERATOR_KEYWORD_Id);
    public static final KtModifierKeywordToken INFIX_KEYWORD = KtModifierKeywordToken.softKeywordModifier("infix", INFIX_KEYWORD_Id);

    public static final KtModifierKeywordToken CONST_KEYWORD = KtModifierKeywordToken.softKeywordModifier("const", CONST_KEYWORD_Id);

    public static final KtModifierKeywordToken SUSPEND_KEYWORD = KtModifierKeywordToken.softKeywordModifier("suspend", SUSPEND_KEYWORD_Id);

    public static final KtModifierKeywordToken EXPECT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("expect", EXPECT_KEYWORD_Id);
    public static final KtModifierKeywordToken ACTUAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("actual", ACTUAL_KEYWORD_Id);


    public static final TokenSet KEYWORDS = TokenSet.create(PACKAGE_KEYWORD, AS_KEYWORD, TYPE_ALIAS_KEYWORD, CLASS_KEYWORD, INTERFACE_KEYWORD,
                                        THIS_KEYWORD, SUPER_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD, FOR_KEYWORD,
                                        NULL_KEYWORD,
                                        TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD,
                                        IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD,
                                        ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, TRY_KEYWORD, WHEN_KEYWORD,
                                        NOT_IN, NOT_IS, AS_SAFE,
                                        TYPEOF_KEYWORD
    );

    public static final TokenSet SOFT_KEYWORDS = TokenSet.create(FILE_KEYWORD, IMPORT_KEYWORD, WHERE_KEYWORD, BY_KEYWORD, GET_KEYWORD,
                                             SET_KEYWORD, ABSTRACT_KEYWORD, ENUM_KEYWORD, CONTRACT_KEYWORD, OPEN_KEYWORD, INNER_KEYWORD,
                                             OVERRIDE_KEYWORD, PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD,
                                             CATCH_KEYWORD, FINALLY_KEYWORD, OUT_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD, REIFIED_KEYWORD,
                                             DYNAMIC_KEYWORD, COMPANION_KEYWORD, CONSTRUCTOR_KEYWORD, INIT_KEYWORD, SEALED_KEYWORD,
                                             FIELD_KEYWORD, PROPERTY_KEYWORD, RECEIVER_KEYWORD, PARAM_KEYWORD, SETPARAM_KEYWORD,
                                             DELEGATE_KEYWORD,
                                             LATEINIT_KEYWORD,
                                             DATA_KEYWORD, INLINE_KEYWORD, NOINLINE_KEYWORD, TAILREC_KEYWORD, EXTERNAL_KEYWORD,
                                             ANNOTATION_KEYWORD, CROSSINLINE_KEYWORD, CONST_KEYWORD, OPERATOR_KEYWORD, INFIX_KEYWORD,
                                             SUSPEND_KEYWORD, EXPECT_KEYWORD, ACTUAL_KEYWORD,
                                             VALUE_KEYWORD, CONTEXT_KEYWORD
    );

    /*
        This array is used in stub serialization:
        1. Do not change order.
        2. If you add an entry or change order, increase stub version.
     */
    public static final KtModifierKeywordToken[] MODIFIER_KEYWORDS_ARRAY =
            new KtModifierKeywordToken[] {
                    ABSTRACT_KEYWORD, ENUM_KEYWORD, CONTRACT_KEYWORD, OPEN_KEYWORD, INNER_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
                    PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, OUT_KEYWORD, IN_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD,
                    REIFIED_KEYWORD, COMPANION_KEYWORD, SEALED_KEYWORD, LATEINIT_KEYWORD,
                    DATA_KEYWORD, INLINE_KEYWORD, NOINLINE_KEYWORD, TAILREC_KEYWORD, EXTERNAL_KEYWORD, ANNOTATION_KEYWORD, CROSSINLINE_KEYWORD,
                    CONST_KEYWORD, OPERATOR_KEYWORD, INFIX_KEYWORD, SUSPEND_KEYWORD,
                    EXPECT_KEYWORD, ACTUAL_KEYWORD, FUN_KEYWORD, VALUE_KEYWORD
            };

    public static final TokenSet MODIFIER_KEYWORDS = TokenSet.create(MODIFIER_KEYWORDS_ARRAY);

    public static final TokenSet TYPE_MODIFIER_KEYWORDS = TokenSet.create(SUSPEND_KEYWORD);
    public static final TokenSet TYPE_ARGUMENT_MODIFIER_KEYWORDS = TokenSet.create(IN_KEYWORD, OUT_KEYWORD);
    public static final TokenSet RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS = TokenSet.create(OUT_KEYWORD, VARARG_KEYWORD);

    public static final TokenSet VISIBILITY_MODIFIERS = TokenSet.create(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD);
    public static final TokenSet MODALITY_MODIFIERS = TokenSet.create(ABSTRACT_KEYWORD, FINAL_KEYWORD, SEALED_KEYWORD, OPEN_KEYWORD);

    public static final TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);

    /**
     * Don't add KDocTokens to COMMENTS TokenSet, because it is used in KotlinParserDefinition.getCommentTokens(),
     * and therefor all COMMENTS tokens will be ignored by PsiBuilder.
     *
     * @see KtPsiUtil#isInComment(com.intellij.psi.PsiElement)
     */
    public static final TokenSet COMMENTS = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT);
    public static final TokenSet WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(COMMENTS, WHITESPACES);

    public static final TokenSet STRINGS = TokenSet.create(CHARACTER_LITERAL, REGULAR_STRING_PART);
    public static final TokenSet OPERATIONS = TokenSet.create(AS_KEYWORD, AS_SAFE, IS_KEYWORD, IN_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, EXCLEXCL, MUL, PLUS,
                                          MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR,
                                          SAFE_ACCESS, ELVIS,
                                          RANGE, RANGE_UNTIL, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,
                                          NOT_IN, NOT_IS,
                                          IDENTIFIER);

    public static final TokenSet AUGMENTED_ASSIGNMENTS = TokenSet.create(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    public static final TokenSet ALL_ASSIGNMENTS = TokenSet.create(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    public static final TokenSet INCREMENT_AND_DECREMENT = TokenSet.create(PLUSPLUS, MINUSMINUS);
}
