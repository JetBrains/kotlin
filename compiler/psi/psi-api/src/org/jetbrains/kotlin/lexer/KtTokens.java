/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;
import org.jetbrains.kotlin.psi.KtPsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds;

import static org.jetbrains.kotlin.KtNodeTypes.*;
import static org.jetbrains.kotlin.syntax.SyntaxIdKt.NO_ID;

public interface KtTokens {
    int INVALID_Id = NO_ID;
    int EOF_Id = KotlinSyntaxTokenIds.EOF;
    int RESERVED_Id = KotlinSyntaxTokenIds.RESERVED;
    int BLOCK_COMMENT_Id = KotlinSyntaxTokenIds.BLOCK_COMMENT;
    int EOL_COMMENT_Id = KotlinSyntaxTokenIds.EOL_COMMENT;
    int SHEBANG_COMMENT_Id = KotlinSyntaxTokenIds.SHEBANG_COMMENT;
    int INTEGER_LITERAL_Id = KotlinSyntaxTokenIds.INTEGER_LITERAL;
    int FLOAT_LITERAL_Id = KotlinSyntaxTokenIds.FLOAT_LITERAL;
    int CHARACTER_LITERAL_Id = KotlinSyntaxTokenIds.CHARACTER_LITERAL;
    int CLOSING_QUOTE_Id = KotlinSyntaxTokenIds.CLOSING_QUOTE;
    int OPEN_QUOTE_Id = KotlinSyntaxTokenIds.OPEN_QUOTE;
    int REGULAR_STRING_PART_Id = KotlinSyntaxTokenIds.REGULAR_STRING_PART;
    int ESCAPE_SEQUENCE_Id = KotlinSyntaxTokenIds.ESCAPE_SEQUENCE;
    int SHORT_TEMPLATE_ENTRY_START_Id = KotlinSyntaxTokenIds.SHORT_TEMPLATE_ENTRY_START;
    int LONG_TEMPLATE_ENTRY_START_Id = KotlinSyntaxTokenIds.LONG_TEMPLATE_ENTRY_START;
    int LONG_TEMPLATE_ENTRY_END_Id = KotlinSyntaxTokenIds.LONG_TEMPLATE_ENTRY_END;
    int DANGLING_NEWLINE_Id = KotlinSyntaxTokenIds.DANGLING_NEWLINE;
    int PACKAGE_KEYWORD_Id = KotlinSyntaxTokenIds.PACKAGE_KEYWORD;
    int AS_KEYWORD_Id = KotlinSyntaxTokenIds.AS_KEYWORD;
    int TYPE_ALIAS_KEYWORD_Id = KotlinSyntaxTokenIds.TYPE_ALIAS_KEYWORD;
    int CLASS_KEYWORD_Id = KotlinSyntaxTokenIds.CLASS_KEYWORD;
    int THIS_KEYWORD_Id = KotlinSyntaxTokenIds.THIS_KEYWORD;
    int SUPER_KEYWORD_Id = KotlinSyntaxTokenIds.SUPER_KEYWORD;
    int VAL_KEYWORD_Id = KotlinSyntaxTokenIds.VAL_KEYWORD;
    int VAR_KEYWORD_Id = KotlinSyntaxTokenIds.VAR_KEYWORD;
    int FUN_KEYWORD_Id = KotlinSyntaxTokenIds.FUN_MODIFIER;
    int FOR_KEYWORD_Id = KotlinSyntaxTokenIds.FOR_KEYWORD;
    int NULL_KEYWORD_Id = KotlinSyntaxTokenIds.NULL_KEYWORD;
    int TRUE_KEYWORD_Id = KotlinSyntaxTokenIds.TRUE_KEYWORD;
    int FALSE_KEYWORD_Id = KotlinSyntaxTokenIds.FALSE_KEYWORD;
    int IS_KEYWORD_Id = KotlinSyntaxTokenIds.IS_KEYWORD;
    int IN_KEYWORD_Id = KotlinSyntaxTokenIds.IN_MODIFIER;
    int THROW_KEYWORD_Id = KotlinSyntaxTokenIds.THROW_KEYWORD;
    int RETURN_KEYWORD_Id = KotlinSyntaxTokenIds.RETURN_KEYWORD;
    int BREAK_KEYWORD_Id = KotlinSyntaxTokenIds.BREAK_KEYWORD;
    int CONTINUE_KEYWORD_Id = KotlinSyntaxTokenIds.CONTINUE_KEYWORD;
    int OBJECT_KEYWORD_Id = KotlinSyntaxTokenIds.OBJECT_KEYWORD;
    int IF_KEYWORD_Id = KotlinSyntaxTokenIds.IF_KEYWORD;
    int TRY_KEYWORD_Id = KotlinSyntaxTokenIds.TRY_KEYWORD;
    int ELSE_KEYWORD_Id = KotlinSyntaxTokenIds.ELSE_KEYWORD;
    int WHILE_KEYWORD_Id = KotlinSyntaxTokenIds.WHILE_KEYWORD;
    int DO_KEYWORD_Id = KotlinSyntaxTokenIds.DO_KEYWORD;
    int WHEN_KEYWORD_Id = KotlinSyntaxTokenIds.WHEN_KEYWORD;
    int INTERFACE_KEYWORD_Id = KotlinSyntaxTokenIds.INTERFACE_KEYWORD;
    int TYPEOF_KEYWORD_Id = KotlinSyntaxTokenIds.TYPEOF_KEYWORD;
    int AS_SAFE_Id = KotlinSyntaxTokenIds.AS_SAFE;
    int IDENTIFIER_Id = KotlinSyntaxTokenIds.IDENTIFIER;
    int FIELD_IDENTIFIER_Id = KotlinSyntaxTokenIds.FIELD_IDENTIFIER;
    int LBRACKET_Id = KotlinSyntaxTokenIds.LBRACKET;
    int RBRACKET_Id = KotlinSyntaxTokenIds.RBRACKET;
    int LBRACE_Id = KotlinSyntaxTokenIds.LBRACE;
    int RBRACE_Id = KotlinSyntaxTokenIds.RBRACE;
    int LPAR_Id = KotlinSyntaxTokenIds.LPAR;
    int RPAR_Id = KotlinSyntaxTokenIds.RPAR;
    int DOT_Id = KotlinSyntaxTokenIds.DOT;
    int PLUSPLUS_Id = KotlinSyntaxTokenIds.PLUSPLUS;
    int MINUSMINUS_Id = KotlinSyntaxTokenIds.MINUSMINUS;
    int MUL_Id = KotlinSyntaxTokenIds.MUL;
    int PLUS_Id = KotlinSyntaxTokenIds.PLUS;
    int MINUS_Id = KotlinSyntaxTokenIds.MINUS;
    int EXCL_Id = KotlinSyntaxTokenIds.EXCL;
    int DIV_Id = KotlinSyntaxTokenIds.DIV;
    int PERC_Id = KotlinSyntaxTokenIds.PERC;
    int LT_Id = KotlinSyntaxTokenIds.LT;
    int GT_Id = KotlinSyntaxTokenIds.GT;
    int LTEQ_Id = KotlinSyntaxTokenIds.LTEQ;
    int GTEQ_Id = KotlinSyntaxTokenIds.GTEQ;
    int EQEQEQ_Id = KotlinSyntaxTokenIds.EQEQEQ;
    int ARROW_Id = KotlinSyntaxTokenIds.ARROW;
    int DOUBLE_ARROW_Id = KotlinSyntaxTokenIds.DOUBLE_ARROW;
    int EXCLEQEQEQ_Id = KotlinSyntaxTokenIds.EXCLEQEQEQ;
    int EQEQ_Id = KotlinSyntaxTokenIds.EQEQ;
    int EXCLEQ_Id = KotlinSyntaxTokenIds.EXCLEQ;
    int EXCLEXCL_Id = KotlinSyntaxTokenIds.EXCLEXCL;
    int ANDAND_Id = KotlinSyntaxTokenIds.ANDAND;
    int AND_Id = KotlinSyntaxTokenIds.AND;
    int OROR_Id = KotlinSyntaxTokenIds.OROR;
    int SAFE_ACCESS_Id = KotlinSyntaxTokenIds.SAFE_ACCESS;
    int ELVIS_Id = KotlinSyntaxTokenIds.ELVIS;
    int QUEST_Id = KotlinSyntaxTokenIds.QUEST;
    int COLONCOLON_Id = KotlinSyntaxTokenIds.COLONCOLON;
    int COLON_Id = KotlinSyntaxTokenIds.COLON;
    int SEMICOLON_Id = KotlinSyntaxTokenIds.SEMICOLON;
    int DOUBLE_SEMICOLON_Id = KotlinSyntaxTokenIds.DOUBLE_SEMICOLON;
    int RANGE_Id = KotlinSyntaxTokenIds.RANGE;
    int RANGE_UNTIL_Id = KotlinSyntaxTokenIds.RANGE_UNTIL;
    int EQ_Id = KotlinSyntaxTokenIds.EQ;
    int MULTEQ_Id = KotlinSyntaxTokenIds.MULTEQ;
    int DIVEQ_Id = KotlinSyntaxTokenIds.DIVEQ;
    int PERCEQ_Id = KotlinSyntaxTokenIds.PERCEQ;
    int PLUSEQ_Id = KotlinSyntaxTokenIds.PLUSEQ;
    int MINUSEQ_Id = KotlinSyntaxTokenIds.MINUSEQ;
    int NOT_IN_Id = KotlinSyntaxTokenIds.NOT_IN;
    int NOT_IS_Id = KotlinSyntaxTokenIds.NOT_IS;
    int HASH_Id = KotlinSyntaxTokenIds.HASH;
    int AT_Id = KotlinSyntaxTokenIds.AT;
    int COMMA_Id = KotlinSyntaxTokenIds.COMMA;
    int EOL_OR_SEMICOLON_Id = KotlinSyntaxTokenIds.EOL_OR_SEMICOLON;
    int FILE_KEYWORD_Id = KotlinSyntaxTokenIds.FILE_KEYWORD;
    int FIELD_KEYWORD_Id = KotlinSyntaxTokenIds.FIELD_KEYWORD;
    int PROPERTY_KEYWORD_Id = KotlinSyntaxTokenIds.PROPERTY_KEYWORD;
    int RECEIVER_KEYWORD_Id = KotlinSyntaxTokenIds.RECEIVER_KEYWORD;
    int PARAM_KEYWORD_Id = KotlinSyntaxTokenIds.PARAM_KEYWORD;
    int SETPARAM_KEYWORD_Id = KotlinSyntaxTokenIds.SETPARAM_KEYWORD;
    int DELEGATE_KEYWORD_Id = KotlinSyntaxTokenIds.DELEGATE_KEYWORD;
    int IMPORT_KEYWORD_Id = KotlinSyntaxTokenIds.IMPORT_KEYWORD;
    int WHERE_KEYWORD_Id = KotlinSyntaxTokenIds.WHERE_KEYWORD;
    int BY_KEYWORD_Id = KotlinSyntaxTokenIds.BY_KEYWORD;
    int GET_KEYWORD_Id = KotlinSyntaxTokenIds.GET_KEYWORD;
    int SET_KEYWORD_Id = KotlinSyntaxTokenIds.SET_KEYWORD;
    int CONSTRUCTOR_KEYWORD_Id = KotlinSyntaxTokenIds.CONSTRUCTOR_KEYWORD;
    int INIT_KEYWORD_Id = KotlinSyntaxTokenIds.INIT_KEYWORD;
    int CONTEXT_KEYWORD_Id = KotlinSyntaxTokenIds.CONTEXT_KEYWORD;
    int CATCH_KEYWORD_Id = KotlinSyntaxTokenIds.CATCH_KEYWORD;
    int DYNAMIC_KEYWORD_Id = KotlinSyntaxTokenIds.DYNAMIC_KEYWORD;
    int FINALLY_KEYWORD_Id = KotlinSyntaxTokenIds.FINALLY_KEYWORD;
    int ABSTRACT_KEYWORD_Id = KotlinSyntaxTokenIds.ABSTRACT_MODIFIER;
    int ENUM_KEYWORD_Id = KotlinSyntaxTokenIds.ENUM_MODIFIER;
    int CONTRACT_KEYWORD_Id = KotlinSyntaxTokenIds.CONTRACT_MODIFIER;
    int OPEN_KEYWORD_Id = KotlinSyntaxTokenIds.OPEN_MODIFIER;
    int INNER_KEYWORD_Id = KotlinSyntaxTokenIds.INNER_MODIFIER;
    int OVERRIDE_KEYWORD_Id = KotlinSyntaxTokenIds.OVERRIDE_MODIFIER;
    int PRIVATE_KEYWORD_Id = KotlinSyntaxTokenIds.PRIVATE_MODIFIER;
    int PUBLIC_KEYWORD_Id = KotlinSyntaxTokenIds.PUBLIC_MODIFIER;
    int INTERNAL_KEYWORD_Id = KotlinSyntaxTokenIds.INTERNAL_MODIFIER;
    int PROTECTED_KEYWORD_Id = KotlinSyntaxTokenIds.PROTECTED_MODIFIER;
    int OUT_KEYWORD_Id = KotlinSyntaxTokenIds.OUT_MODIFIER;
    int VARARG_KEYWORD_Id = KotlinSyntaxTokenIds.VARARG_MODIFIER;
    int REIFIED_KEYWORD_Id = KotlinSyntaxTokenIds.REIFIED_MODIFIER;
    int COMPANION_KEYWORD_Id = KotlinSyntaxTokenIds.COMPANION_MODIFIER;
    int SEALED_KEYWORD_Id = KotlinSyntaxTokenIds.SEALED_MODIFIER;
    int FINAL_KEYWORD_Id = KotlinSyntaxTokenIds.FINAL_MODIFIER;
    int LATEINIT_KEYWORD_Id = KotlinSyntaxTokenIds.LATEINIT_MODIFIER;
    int DATA_KEYWORD_Id = KotlinSyntaxTokenIds.DATA_MODIFIER;
    int VALUE_KEYWORD_Id = KotlinSyntaxTokenIds.VALUE_MODIFIER;
    int INLINE_KEYWORD_Id = KotlinSyntaxTokenIds.INLINE_MODIFIER;
    int NOINLINE_KEYWORD_Id = KotlinSyntaxTokenIds.NOINLINE_MODIFIER;
    int TAILREC_KEYWORD_Id = KotlinSyntaxTokenIds.TAILREC_MODIFIER;
    int EXTERNAL_KEYWORD_Id = KotlinSyntaxTokenIds.EXTERNAL_MODIFIER;
    int ANNOTATION_KEYWORD_Id = KotlinSyntaxTokenIds.ANNOTATION_MODIFIER;
    int CROSSINLINE_KEYWORD_Id = KotlinSyntaxTokenIds.CROSSINLINE_MODIFIER;
    int OPERATOR_KEYWORD_Id = KotlinSyntaxTokenIds.OPERATOR_MODIFIER;
    int INFIX_KEYWORD_Id = KotlinSyntaxTokenIds.INFIX_MODIFIER;
    int CONST_KEYWORD_Id = KotlinSyntaxTokenIds.CONST_MODIFIER;
    int SUSPEND_KEYWORD_Id = KotlinSyntaxTokenIds.SUSPEND_MODIFIER;
    int EXPECT_KEYWORD_Id = KotlinSyntaxTokenIds.EXPECT_MODIFIER;
    int ACTUAL_KEYWORD_Id = KotlinSyntaxTokenIds.ACTUAL_MODIFIER;
    int INTERPOLATION_PREFIX_Id = KotlinSyntaxTokenIds.INTERPOLATION_PREFIX;
    int ALL_KEYWORD_Id = KotlinSyntaxTokenIds.ALL_KEYWORD;
    int ERROR_KEYWORD_Id = KotlinSyntaxTokenIds.ERROR_MODIFIER;
    int OR_Id = KotlinSyntaxTokenIds.OR;
    int ERROR_SAFE_ACCESS_Id = KotlinSyntaxTokenIds.ERROR_SAFE_ACCESS;

    @NotNull KtToken EOF   = new KtToken("EOF", EOF_Id);

    @NotNull KtToken RESERVED    = new KtToken("RESERVED", RESERVED_Id);

    @NotNull KtToken BLOCK_COMMENT     = new KtToken("BLOCK_COMMENT", BLOCK_COMMENT_Id);
    @NotNull KtToken EOL_COMMENT       = new KtToken("EOL_COMMENT", EOL_COMMENT_Id);
    @NotNull KtToken SHEBANG_COMMENT   = new KtToken("SHEBANG_COMMENT", SHEBANG_COMMENT_Id);

    @NotNull IElementType DOC_COMMENT   = KDocTokens.KDOC;

    @NotNull IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    @NotNull KtToken INTEGER_LITERAL    = new KtToken("INTEGER_LITERAL", INTEGER_LITERAL_Id);
    @NotNull KtToken FLOAT_LITERAL      = new KtToken("FLOAT_CONSTANT", FLOAT_LITERAL_Id);
    @NotNull KtToken CHARACTER_LITERAL  = new KtToken("CHARACTER_LITERAL", CHARACTER_LITERAL_Id);

    @NotNull KtToken INTERPOLATION_PREFIX = new KtToken("INTERPOLATION_PREFIX", INTERPOLATION_PREFIX_Id);
    @NotNull KtToken CLOSING_QUOTE = new KtToken("CLOSING_QUOTE", CLOSING_QUOTE_Id);
    @NotNull KtToken OPEN_QUOTE = new KtToken("OPEN_QUOTE", OPEN_QUOTE_Id);
    @NotNull KtToken REGULAR_STRING_PART = new KtToken("REGULAR_STRING_PART", REGULAR_STRING_PART_Id);
    @NotNull KtToken ESCAPE_SEQUENCE = new KtToken("ESCAPE_SEQUENCE", ESCAPE_SEQUENCE_Id);
    @NotNull KtToken SHORT_TEMPLATE_ENTRY_START = new KtToken("SHORT_TEMPLATE_ENTRY_START", SHORT_TEMPLATE_ENTRY_START_Id);
    @NotNull KtToken LONG_TEMPLATE_ENTRY_START = new KtToken("LONG_TEMPLATE_ENTRY_START", LONG_TEMPLATE_ENTRY_START_Id);
    @NotNull KtToken LONG_TEMPLATE_ENTRY_END = new KtToken("LONG_TEMPLATE_ENTRY_END", LONG_TEMPLATE_ENTRY_END_Id);
    @NotNull KtToken DANGLING_NEWLINE = new KtToken("DANGLING_NEWLINE", DANGLING_NEWLINE_Id);

    @NotNull KtKeywordToken PACKAGE_KEYWORD          = KtKeywordToken.keyword("package", PACKAGE_KEYWORD_Id);
    @NotNull KtKeywordToken AS_KEYWORD               = KtKeywordToken.keyword("as", AS_KEYWORD_Id);
    @NotNull KtKeywordToken TYPE_ALIAS_KEYWORD       = KtKeywordToken.keyword("typealias", TYPE_ALIAS_KEYWORD_Id);
    @NotNull KtKeywordToken CLASS_KEYWORD            = KtKeywordToken.keyword("class", CLASS_KEYWORD_Id);
    @NotNull KtKeywordToken THIS_KEYWORD             = KtKeywordToken.keyword("this", THIS_KEYWORD_Id);
    @NotNull KtKeywordToken SUPER_KEYWORD            = KtKeywordToken.keyword("super", SUPER_KEYWORD_Id);
    @NotNull KtKeywordToken VAL_KEYWORD              = KtKeywordToken.keyword("val", VAL_KEYWORD_Id);
    @NotNull KtKeywordToken VAR_KEYWORD              = KtKeywordToken.keyword("var", VAR_KEYWORD_Id);
    @NotNull KtModifierKeywordToken FUN_KEYWORD      = KtModifierKeywordToken.keywordModifier("fun", FUN_KEYWORD_Id);
    @NotNull KtKeywordToken FOR_KEYWORD              = KtKeywordToken.keyword("for", FOR_KEYWORD_Id);
    @NotNull KtKeywordToken NULL_KEYWORD             = KtKeywordToken.keyword("null", NULL_KEYWORD_Id);
    @NotNull KtKeywordToken TRUE_KEYWORD             = KtKeywordToken.keyword("true", TRUE_KEYWORD_Id);
    @NotNull KtKeywordToken FALSE_KEYWORD            = KtKeywordToken.keyword("false", FALSE_KEYWORD_Id);
    @NotNull KtKeywordToken IS_KEYWORD               = KtKeywordToken.keyword("is", IS_KEYWORD_Id);
    @NotNull KtModifierKeywordToken IN_KEYWORD       = KtModifierKeywordToken.keywordModifier("in", IN_KEYWORD_Id);
    @NotNull KtKeywordToken THROW_KEYWORD            = KtKeywordToken.keyword("throw", THROW_KEYWORD_Id);
    @NotNull KtKeywordToken RETURN_KEYWORD           = KtKeywordToken.keyword("return", RETURN_KEYWORD_Id);
    @NotNull KtKeywordToken BREAK_KEYWORD            = KtKeywordToken.keyword("break", BREAK_KEYWORD_Id);
    @NotNull KtKeywordToken CONTINUE_KEYWORD         = KtKeywordToken.keyword("continue", CONTINUE_KEYWORD_Id);
    @NotNull KtKeywordToken OBJECT_KEYWORD           = KtKeywordToken.keyword("object", OBJECT_KEYWORD_Id);
    @NotNull KtKeywordToken IF_KEYWORD               = KtKeywordToken.keyword("if", IF_KEYWORD_Id);
    @NotNull KtKeywordToken TRY_KEYWORD              = KtKeywordToken.keyword("try", TRY_KEYWORD_Id);
    @NotNull KtKeywordToken ELSE_KEYWORD             = KtKeywordToken.keyword("else", ELSE_KEYWORD_Id);
    @NotNull KtKeywordToken WHILE_KEYWORD            = KtKeywordToken.keyword("while", WHILE_KEYWORD_Id);
    @NotNull KtKeywordToken DO_KEYWORD               = KtKeywordToken.keyword("do", DO_KEYWORD_Id);
    @NotNull KtKeywordToken WHEN_KEYWORD             = KtKeywordToken.keyword("when", WHEN_KEYWORD_Id);
    @NotNull KtKeywordToken INTERFACE_KEYWORD        = KtKeywordToken.keyword("interface", INTERFACE_KEYWORD_Id);

    // Reserved for future use:
    @NotNull KtKeywordToken TYPEOF_KEYWORD           = KtKeywordToken.keyword("typeof", TYPEOF_KEYWORD_Id);

    @NotNull KtToken AS_SAFE = KtKeywordToken.keyword("AS_SAFE", "as?", AS_SAFE_Id);

    @NotNull KtToken IDENTIFIER = new KtToken("IDENTIFIER", IDENTIFIER_Id);

    @NotNull KtToken FIELD_IDENTIFIER = new KtToken("FIELD_IDENTIFIER", FIELD_IDENTIFIER_Id);
    @NotNull KtSingleValueToken LBRACKET    = new KtSingleValueToken("LBRACKET", "[", LBRACKET_Id);
    @NotNull KtSingleValueToken RBRACKET    = new KtSingleValueToken("RBRACKET", "]", RBRACKET_Id);
    @NotNull KtSingleValueToken LBRACE      = new KtSingleValueToken("LBRACE", "{", LBRACE_Id);
    @NotNull KtSingleValueToken RBRACE      = new KtSingleValueToken("RBRACE", "}", RBRACE_Id);
    @NotNull KtSingleValueToken LPAR        = new KtSingleValueToken("LPAR", "(", LPAR_Id);
    @NotNull KtSingleValueToken RPAR        = new KtSingleValueToken("RPAR", ")", RPAR_Id);
    @NotNull KtSingleValueToken DOT         = new KtSingleValueToken("DOT", ".", DOT_Id);
    @NotNull KtSingleValueToken PLUSPLUS    = new KtSingleValueToken("PLUSPLUS", "++", PLUSPLUS_Id);
    @NotNull KtSingleValueToken MINUSMINUS  = new KtSingleValueToken("MINUSMINUS", "--", MINUSMINUS_Id);
    @NotNull KtSingleValueToken MUL         = new KtSingleValueToken("MUL", "*", MUL_Id);
    @NotNull KtSingleValueToken PLUS        = new KtSingleValueToken("PLUS", "+", PLUS_Id);
    @NotNull KtSingleValueToken MINUS       = new KtSingleValueToken("MINUS", "-", MINUS_Id);
    @NotNull KtSingleValueToken EXCL        = new KtSingleValueToken("EXCL", "!", EXCL_Id);
    @NotNull KtSingleValueToken DIV         = new KtSingleValueToken("DIV", "/", DIV_Id);
    @NotNull KtSingleValueToken PERC        = new KtSingleValueToken("PERC", "%", PERC_Id);
    @NotNull KtSingleValueToken LT          = new KtSingleValueToken("LT", "<", LT_Id);
    @NotNull KtSingleValueToken GT          = new KtSingleValueToken("GT", ">", GT_Id);
    @NotNull KtSingleValueToken LTEQ        = new KtSingleValueToken("LTEQ", "<=", LTEQ_Id);
    @NotNull KtSingleValueToken GTEQ        = new KtSingleValueToken("GTEQ", ">=", GTEQ_Id);
    @NotNull KtSingleValueToken EQEQEQ      = new KtSingleValueToken("EQEQEQ", "===", EQEQEQ_Id);
    @NotNull KtSingleValueToken ARROW       = new KtSingleValueToken("ARROW", "->", ARROW_Id);
    @NotNull KtSingleValueToken DOUBLE_ARROW       = new KtSingleValueToken("DOUBLE_ARROW", "=>", DOUBLE_ARROW_Id);
    @NotNull KtSingleValueToken EXCLEQEQEQ  = new KtSingleValueToken("EXCLEQEQEQ", "!==", EXCLEQEQEQ_Id);
    @NotNull KtSingleValueToken EQEQ        = new KtSingleValueToken("EQEQ", "==", EQEQ_Id);
    @NotNull KtSingleValueToken EXCLEQ      = new KtSingleValueToken("EXCLEQ", "!=", EXCLEQ_Id);
    @NotNull KtSingleValueToken EXCLEXCL    = new KtSingleValueToken("EXCLEXCL", "!!", EXCLEXCL_Id);
    @NotNull KtSingleValueToken ANDAND      = new KtSingleValueToken("ANDAND", "&&", ANDAND_Id);
    @NotNull KtSingleValueToken AND         = new KtSingleValueToken("AND", "&", AND_Id);
    @NotNull KtSingleValueToken OROR        = new KtSingleValueToken("OROR", "||", OROR_Id);
    @NotNull KtSingleValueToken OR          = new KtSingleValueToken("OR", "|", OR_Id);
    @NotNull KtSingleValueToken SAFE_ACCESS = new KtSingleValueToken("SAFE_ACCESS", "?.", SAFE_ACCESS_Id);
    @NotNull KtSingleValueToken ERROR_SAFE_ACCESS = new KtSingleValueToken("ERROR_SAFE_ACCESS", "|.", ERROR_SAFE_ACCESS_Id);
    @NotNull KtSingleValueToken ELVIS       = new KtSingleValueToken("ELVIS", "?:", ELVIS_Id);
    @NotNull KtSingleValueToken QUEST       = new KtSingleValueToken("QUEST", "?", QUEST_Id);
    @NotNull KtSingleValueToken COLONCOLON  = new KtSingleValueToken("COLONCOLON", "::", COLONCOLON_Id);
    @NotNull KtSingleValueToken COLON       = new KtSingleValueToken("COLON", ":", COLON_Id);
    @NotNull KtSingleValueToken SEMICOLON   = new KtSingleValueToken("SEMICOLON", ";", SEMICOLON_Id);
    @NotNull KtSingleValueToken DOUBLE_SEMICOLON   = new KtSingleValueToken("DOUBLE_SEMICOLON", ";;", DOUBLE_SEMICOLON_Id);
    @NotNull KtSingleValueToken RANGE       = new KtSingleValueToken("RANGE", "..", RANGE_Id);
    @NotNull KtSingleValueToken RANGE_UNTIL       = new KtSingleValueToken("RANGE_UNTIL", "..<", RANGE_UNTIL_Id);
    @NotNull KtSingleValueToken EQ          = new KtSingleValueToken("EQ", "=", EQ_Id);
    @NotNull KtSingleValueToken MULTEQ      = new KtSingleValueToken("MULTEQ", "*=", MULTEQ_Id);
    @NotNull KtSingleValueToken DIVEQ       = new KtSingleValueToken("DIVEQ", "/=", DIVEQ_Id);
    @NotNull KtSingleValueToken PERCEQ      = new KtSingleValueToken("PERCEQ", "%=", PERCEQ_Id);
    @NotNull KtSingleValueToken PLUSEQ      = new KtSingleValueToken("PLUSEQ", "+=", PLUSEQ_Id);
    @NotNull KtSingleValueToken MINUSEQ     = new KtSingleValueToken("MINUSEQ", "-=", MINUSEQ_Id);
    @NotNull KtKeywordToken NOT_IN      = KtKeywordToken.keyword("NOT_IN", "!in", NOT_IN_Id);
    @NotNull KtKeywordToken NOT_IS      = KtKeywordToken.keyword("NOT_IS", "!is", NOT_IS_Id);
    @NotNull KtSingleValueToken HASH        = new KtSingleValueToken("HASH", "#", HASH_Id);
    @NotNull KtSingleValueToken AT          = new KtSingleValueToken("AT", "@", AT_Id);

    @NotNull KtSingleValueToken COMMA       = new KtSingleValueToken("COMMA", ",", COMMA_Id);

    @NotNull KtToken EOL_OR_SEMICOLON   = new KtToken("EOL_OR_SEMICOLON", EOL_OR_SEMICOLON_Id);
    @NotNull KtKeywordToken ALL_KEYWORD    = KtKeywordToken.softKeyword("all", ALL_KEYWORD_Id);
    @NotNull KtKeywordToken FILE_KEYWORD    = KtKeywordToken.softKeyword("file", FILE_KEYWORD_Id);
    @NotNull KtKeywordToken FIELD_KEYWORD     = KtKeywordToken.softKeyword("field", FIELD_KEYWORD_Id);
    @NotNull KtKeywordToken PROPERTY_KEYWORD     = KtKeywordToken.softKeyword("property", PROPERTY_KEYWORD_Id);
    @NotNull KtKeywordToken RECEIVER_KEYWORD     = KtKeywordToken.softKeyword("receiver", RECEIVER_KEYWORD_Id);
    @NotNull KtKeywordToken PARAM_KEYWORD     = KtKeywordToken.softKeyword("param", PARAM_KEYWORD_Id);
    @NotNull KtKeywordToken SETPARAM_KEYWORD  = KtKeywordToken.softKeyword("setparam", SETPARAM_KEYWORD_Id);
    @NotNull KtKeywordToken DELEGATE_KEYWORD  = KtKeywordToken.softKeyword("delegate", DELEGATE_KEYWORD_Id);
    @NotNull KtKeywordToken IMPORT_KEYWORD    = KtKeywordToken.softKeyword("import", IMPORT_KEYWORD_Id);
    @NotNull KtKeywordToken WHERE_KEYWORD     = KtKeywordToken.softKeyword("where", WHERE_KEYWORD_Id);
    @NotNull KtKeywordToken BY_KEYWORD        = KtKeywordToken.softKeyword("by", BY_KEYWORD_Id);
    @NotNull KtKeywordToken GET_KEYWORD       = KtKeywordToken.softKeyword("get", GET_KEYWORD_Id);
    @NotNull KtKeywordToken SET_KEYWORD       = KtKeywordToken.softKeyword("set", SET_KEYWORD_Id);
    @NotNull KtKeywordToken CONSTRUCTOR_KEYWORD = KtKeywordToken.softKeyword("constructor", CONSTRUCTOR_KEYWORD_Id);
    @NotNull KtKeywordToken INIT_KEYWORD        = KtKeywordToken.softKeyword("init", INIT_KEYWORD_Id);
    @NotNull KtKeywordToken CONTEXT_KEYWORD     = KtKeywordToken.softKeyword("context", CONTEXT_KEYWORD_Id);
    @NotNull KtKeywordToken CATCH_KEYWORD     = KtKeywordToken.softKeyword("catch", CATCH_KEYWORD_Id);
    @NotNull KtKeywordToken DYNAMIC_KEYWORD   = KtKeywordToken.softKeyword("dynamic", DYNAMIC_KEYWORD_Id);
    @NotNull KtKeywordToken FINALLY_KEYWORD   = KtKeywordToken.softKeyword("finally", FINALLY_KEYWORD_Id);

    @NotNull KtModifierKeywordToken ABSTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("abstract", ABSTRACT_KEYWORD_Id);
    @NotNull KtModifierKeywordToken ENUM_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("enum", ENUM_KEYWORD_Id);
    @NotNull KtModifierKeywordToken CONTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("contract", CONTRACT_KEYWORD_Id);
    @NotNull KtModifierKeywordToken OPEN_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("open", OPEN_KEYWORD_Id);
    @NotNull KtModifierKeywordToken INNER_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("inner", INNER_KEYWORD_Id);
    @NotNull KtModifierKeywordToken OVERRIDE_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("override", OVERRIDE_KEYWORD_Id);
    @NotNull KtModifierKeywordToken PRIVATE_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("private", PRIVATE_KEYWORD_Id);
    @NotNull KtModifierKeywordToken PUBLIC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("public", PUBLIC_KEYWORD_Id);
    @NotNull KtModifierKeywordToken INTERNAL_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("internal", INTERNAL_KEYWORD_Id);
    @NotNull KtModifierKeywordToken PROTECTED_KEYWORD = KtModifierKeywordToken.softKeywordModifier("protected", PROTECTED_KEYWORD_Id);
    @NotNull KtModifierKeywordToken OUT_KEYWORD       = KtModifierKeywordToken.softKeywordModifier("out", OUT_KEYWORD_Id);
    @NotNull KtModifierKeywordToken VARARG_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("vararg", VARARG_KEYWORD_Id);
    @NotNull KtModifierKeywordToken REIFIED_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("reified", REIFIED_KEYWORD_Id);
    @NotNull KtModifierKeywordToken COMPANION_KEYWORD = KtModifierKeywordToken.softKeywordModifier("companion", COMPANION_KEYWORD_Id);
    @NotNull KtModifierKeywordToken SEALED_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("sealed", SEALED_KEYWORD_Id);
    @NotNull KtModifierKeywordToken ERROR_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("error", ERROR_KEYWORD_Id);

    @Deprecated @NotNull KtModifierKeywordToken DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD;

    @NotNull KtModifierKeywordToken FINAL_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("final", FINAL_KEYWORD_Id);

    @NotNull KtModifierKeywordToken LATEINIT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("lateinit", LATEINIT_KEYWORD_Id);

    @NotNull KtModifierKeywordToken DATA_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("data", DATA_KEYWORD_Id);
    @NotNull KtModifierKeywordToken VALUE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("value", VALUE_KEYWORD_Id);
    @NotNull KtModifierKeywordToken INLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("inline", INLINE_KEYWORD_Id);
    @NotNull KtModifierKeywordToken NOINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("noinline", NOINLINE_KEYWORD_Id);
    @NotNull KtModifierKeywordToken TAILREC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("tailrec", TAILREC_KEYWORD_Id);
    @NotNull KtModifierKeywordToken EXTERNAL_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("external", EXTERNAL_KEYWORD_Id);
    @NotNull KtModifierKeywordToken ANNOTATION_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("annotation", ANNOTATION_KEYWORD_Id);
    @NotNull KtModifierKeywordToken CROSSINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("crossinline", CROSSINLINE_KEYWORD_Id);
    @NotNull KtModifierKeywordToken OPERATOR_KEYWORD = KtModifierKeywordToken.softKeywordModifier("operator", OPERATOR_KEYWORD_Id);
    @NotNull KtModifierKeywordToken INFIX_KEYWORD = KtModifierKeywordToken.softKeywordModifier("infix", INFIX_KEYWORD_Id);

    @NotNull KtModifierKeywordToken CONST_KEYWORD = KtModifierKeywordToken.softKeywordModifier("const", CONST_KEYWORD_Id);

    @NotNull KtModifierKeywordToken SUSPEND_KEYWORD = KtModifierKeywordToken.softKeywordModifier("suspend", SUSPEND_KEYWORD_Id);

    @NotNull KtModifierKeywordToken EXPECT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("expect", EXPECT_KEYWORD_Id);
    @NotNull KtModifierKeywordToken ACTUAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("actual", ACTUAL_KEYWORD_Id);


    @NotNull
    TokenSet KEYWORDS = TokenSet.create(PACKAGE_KEYWORD, AS_KEYWORD, TYPE_ALIAS_KEYWORD, CLASS_KEYWORD, INTERFACE_KEYWORD,
                                        THIS_KEYWORD, SUPER_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD, FOR_KEYWORD,
                                        NULL_KEYWORD,
                                        TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD,
                                        IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD,
                                        ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, TRY_KEYWORD, WHEN_KEYWORD,
                                        NOT_IN, NOT_IS, AS_SAFE,
                                        TYPEOF_KEYWORD
    );

    @NotNull
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
                                             VALUE_KEYWORD, CONTEXT_KEYWORD, ERROR_KEYWORD
    );

    /**
     * Canonically ordered array of all modifier keywords.
     * <p>
     * <b>IMPORTANT:</b> modifiers are used in stub serialization, so
     * <ul>
     *   <li>Do <b>NOT</b> change the order of this array unless necessary</li>
     *   <li>If you add an entry or rearrange the order, be sure to increase {@link org.jetbrains.kotlin.psi.stubs.KotlinStubVersions the stub version}.</li>
     * </ul>
     *
     * @see org.jetbrains.kotlin.psi.stubs.KotlinStubVersions
     */
    KtModifierKeywordToken[] MODIFIER_KEYWORDS_ARRAY =
            new KtModifierKeywordToken[] {
                    PUBLIC_KEYWORD, PROTECTED_KEYWORD, PRIVATE_KEYWORD, INTERNAL_KEYWORD,
                    EXPECT_KEYWORD, ACTUAL_KEYWORD,
                    FINAL_KEYWORD, OPEN_KEYWORD, ABSTRACT_KEYWORD, SEALED_KEYWORD,
                    CONST_KEYWORD,
                    EXTERNAL_KEYWORD,
                    OVERRIDE_KEYWORD,
                    LATEINIT_KEYWORD,
                    TAILREC_KEYWORD,
                    VARARG_KEYWORD,
                    SUSPEND_KEYWORD,
                    INNER_KEYWORD,
                    ENUM_KEYWORD, ANNOTATION_KEYWORD, FUN_KEYWORD,
                    COMPANION_KEYWORD,
                    INLINE_KEYWORD,
                    VALUE_KEYWORD,
                    INFIX_KEYWORD,
                    OPERATOR_KEYWORD,
                    DATA_KEYWORD,
                    ERROR_KEYWORD,

                    OUT_KEYWORD, IN_KEYWORD,
                    REIFIED_KEYWORD,
                    NOINLINE_KEYWORD,
                    CROSSINLINE_KEYWORD,
            };

    @NotNull TokenSet MODIFIER_KEYWORDS = TokenSet.create(MODIFIER_KEYWORDS_ARRAY);

    @NotNull TokenSet TYPE_MODIFIER_KEYWORDS = TokenSet.create(SUSPEND_KEYWORD);
    @NotNull TokenSet TYPE_ARGUMENT_MODIFIER_KEYWORDS = TokenSet.create(IN_KEYWORD, OUT_KEYWORD);
    @NotNull TokenSet RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS = TokenSet.create(OUT_KEYWORD, VARARG_KEYWORD);

    @NotNull TokenSet VISIBILITY_MODIFIERS = TokenSet.create(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD);
    @NotNull TokenSet MODALITY_MODIFIERS = TokenSet.create(ABSTRACT_KEYWORD, FINAL_KEYWORD, SEALED_KEYWORD, OPEN_KEYWORD);

    @NotNull TokenSet WHITESPACES = TokenSet.create(TokenType.WHITE_SPACE);

    /**
     * Don't add KDocTokens to COMMENTS TokenSet, because it is used in KotlinParserDefinition.getCommentTokens(),
     * and therefor all COMMENTS tokens will be ignored by PsiBuilder.
     *
     * @see KtPsiUtil#isInComment(com.intellij.psi.PsiElement)
     */
    @NotNull TokenSet COMMENTS = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT);
    @NotNull TokenSet WHITE_SPACE_OR_COMMENT_BIT_SET = TokenSet.orSet(COMMENTS, WHITESPACES);

    @NotNull TokenSet STRINGS = TokenSet.create(CHARACTER_LITERAL, REGULAR_STRING_PART);

    @NotNull
    TokenSet OPERATIONS = TokenSet.create(AS_KEYWORD, AS_SAFE, IS_KEYWORD, IN_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, EXCLEXCL, MUL, PLUS,
                                          MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR,
                                          SAFE_ACCESS, ERROR_SAFE_ACCESS, ELVIS,
                                          RANGE, RANGE_UNTIL, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,
                                          NOT_IN, NOT_IS,
                                          IDENTIFIER);

    @NotNull TokenSet AUGMENTED_ASSIGNMENTS = TokenSet.create(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    @NotNull TokenSet ALL_ASSIGNMENTS = TokenSet.create(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ);
    @NotNull TokenSet INCREMENT_AND_DECREMENT = TokenSet.create(PLUSPLUS, MINUSMINUS);
    @NotNull TokenSet QUALIFIED_ACCESS = TokenSet.create(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION, ERROR_SAFE_ACCESS_EXPRESSION);
    @NotNull TokenSet VAL_VAR = TokenSet.create(VAL_KEYWORD, VAR_KEYWORD);
    @NotNull TokenSet ALL_SAFE_ACCESSES = TokenSet.create(SAFE_ACCESS, ERROR_SAFE_ACCESS);
}
