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
import org.jetbrains.kotlin.syntax.SyntaxTokenIds;

import static org.jetbrains.kotlin.KtNodeTypes.*;

public interface KtTokens {
    int INVALID_Id = 0;
    int EOF_Id = SyntaxTokenIds.EOF;
    int RESERVED_Id = SyntaxTokenIds.RESERVED;
    int BLOCK_COMMENT_Id = SyntaxTokenIds.BLOCK_COMMENT;
    int EOL_COMMENT_Id = SyntaxTokenIds.EOL_COMMENT;
    int SHEBANG_COMMENT_Id = SyntaxTokenIds.SHEBANG_COMMENT;
    int INTEGER_LITERAL_Id = SyntaxTokenIds.INTEGER_LITERAL;
    int FLOAT_LITERAL_Id = SyntaxTokenIds.FLOAT_LITERAL;
    int CHARACTER_LITERAL_Id = SyntaxTokenIds.CHARACTER_LITERAL;
    int CLOSING_QUOTE_Id = SyntaxTokenIds.CLOSING_QUOTE;
    int OPEN_QUOTE_Id = SyntaxTokenIds.OPEN_QUOTE;
    int REGULAR_STRING_PART_Id = SyntaxTokenIds.REGULAR_STRING_PART;
    int ESCAPE_SEQUENCE_Id = SyntaxTokenIds.ESCAPE_SEQUENCE;
    int SHORT_TEMPLATE_ENTRY_START_Id = SyntaxTokenIds.SHORT_TEMPLATE_ENTRY_START;
    int LONG_TEMPLATE_ENTRY_START_Id = SyntaxTokenIds.LONG_TEMPLATE_ENTRY_START;
    int LONG_TEMPLATE_ENTRY_END_Id = SyntaxTokenIds.LONG_TEMPLATE_ENTRY_END;
    int DANGLING_NEWLINE_Id = SyntaxTokenIds.DANGLING_NEWLINE;
    int PACKAGE_KEYWORD_Id = SyntaxTokenIds.PACKAGE_KEYWORD;
    int AS_KEYWORD_Id = SyntaxTokenIds.AS_KEYWORD;
    int TYPE_ALIAS_KEYWORD_Id = SyntaxTokenIds.TYPE_ALIAS_KEYWORD;
    int CLASS_KEYWORD_Id = SyntaxTokenIds.CLASS_KEYWORD;
    int THIS_KEYWORD_Id = SyntaxTokenIds.THIS_KEYWORD;
    int SUPER_KEYWORD_Id = SyntaxTokenIds.SUPER_KEYWORD;
    int VAL_KEYWORD_Id = SyntaxTokenIds.VAL_KEYWORD;
    int VAR_KEYWORD_Id = SyntaxTokenIds.VAR_KEYWORD;
    int FUN_KEYWORD_Id = SyntaxTokenIds.FUN_MODIFIER;
    int FOR_KEYWORD_Id = SyntaxTokenIds.FOR_KEYWORD;
    int NULL_KEYWORD_Id = SyntaxTokenIds.NULL_KEYWORD;
    int TRUE_KEYWORD_Id = SyntaxTokenIds.TRUE_KEYWORD;
    int FALSE_KEYWORD_Id = SyntaxTokenIds.FALSE_KEYWORD;
    int IS_KEYWORD_Id = SyntaxTokenIds.IS_KEYWORD;
    int IN_KEYWORD_Id = SyntaxTokenIds.IN_MODIFIER;
    int THROW_KEYWORD_Id = SyntaxTokenIds.THROW_KEYWORD;
    int RETURN_KEYWORD_Id = SyntaxTokenIds.RETURN_KEYWORD;
    int BREAK_KEYWORD_Id = SyntaxTokenIds.BREAK_KEYWORD;
    int CONTINUE_KEYWORD_Id = SyntaxTokenIds.CONTINUE_KEYWORD;
    int OBJECT_KEYWORD_Id = SyntaxTokenIds.OBJECT_KEYWORD;
    int IF_KEYWORD_Id = SyntaxTokenIds.IF_KEYWORD;
    int TRY_KEYWORD_Id = SyntaxTokenIds.TRY_KEYWORD;
    int ELSE_KEYWORD_Id = SyntaxTokenIds.ELSE_KEYWORD;
    int WHILE_KEYWORD_Id = SyntaxTokenIds.WHILE_KEYWORD;
    int DO_KEYWORD_Id = SyntaxTokenIds.DO_KEYWORD;
    int WHEN_KEYWORD_Id = SyntaxTokenIds.WHEN_KEYWORD;
    int INTERFACE_KEYWORD_Id = SyntaxTokenIds.INTERFACE_KEYWORD;
    int TYPEOF_KEYWORD_Id = SyntaxTokenIds.TYPEOF_KEYWORD;
    int AS_SAFE_Id = SyntaxTokenIds.AS_SAFE;
    int IDENTIFIER_Id = SyntaxTokenIds.IDENTIFIER;
    int FIELD_IDENTIFIER_Id = SyntaxTokenIds.FIELD_IDENTIFIER;
    int LBRACKET_Id = SyntaxTokenIds.LBRACKET;
    int RBRACKET_Id = SyntaxTokenIds.RBRACKET;
    int LBRACE_Id = SyntaxTokenIds.LBRACE;
    int RBRACE_Id = SyntaxTokenIds.RBRACE;
    int LPAR_Id = SyntaxTokenIds.LPAR;
    int RPAR_Id = SyntaxTokenIds.RPAR;
    int DOT_Id = SyntaxTokenIds.DOT;
    int PLUSPLUS_Id = SyntaxTokenIds.PLUSPLUS;
    int MINUSMINUS_Id = SyntaxTokenIds.MINUSMINUS;
    int MUL_Id = SyntaxTokenIds.MUL;
    int PLUS_Id = SyntaxTokenIds.PLUS;
    int MINUS_Id = SyntaxTokenIds.MINUS;
    int EXCL_Id = SyntaxTokenIds.EXCL;
    int DIV_Id = SyntaxTokenIds.DIV;
    int PERC_Id = SyntaxTokenIds.PERC;
    int LT_Id = SyntaxTokenIds.LT;
    int GT_Id = SyntaxTokenIds.GT;
    int LTEQ_Id = SyntaxTokenIds.LTEQ;
    int GTEQ_Id = SyntaxTokenIds.GTEQ;
    int EQEQEQ_Id = SyntaxTokenIds.EQEQEQ;
    int ARROW_Id = SyntaxTokenIds.ARROW;
    int DOUBLE_ARROW_Id = SyntaxTokenIds.DOUBLE_ARROW;
    int EXCLEQEQEQ_Id = SyntaxTokenIds.EXCLEQEQEQ;
    int EQEQ_Id = SyntaxTokenIds.EQEQ;
    int EXCLEQ_Id = SyntaxTokenIds.EXCLEQ;
    int EXCLEXCL_Id = SyntaxTokenIds.EXCLEXCL;
    int ANDAND_Id = SyntaxTokenIds.ANDAND;
    int AND_Id = SyntaxTokenIds.AND;
    int OROR_Id = SyntaxTokenIds.OROR;
    int SAFE_ACCESS_Id = SyntaxTokenIds.SAFE_ACCESS;
    int ELVIS_Id = SyntaxTokenIds.ELVIS;
    int QUEST_Id = SyntaxTokenIds.QUEST;
    int COLONCOLON_Id = SyntaxTokenIds.COLONCOLON;
    int COLON_Id = SyntaxTokenIds.COLON;
    int SEMICOLON_Id = SyntaxTokenIds.SEMICOLON;
    int DOUBLE_SEMICOLON_Id = SyntaxTokenIds.DOUBLE_SEMICOLON;
    int RANGE_Id = SyntaxTokenIds.RANGE;
    int RANGE_UNTIL_Id = SyntaxTokenIds.RANGE_UNTIL;
    int EQ_Id = SyntaxTokenIds.EQ;
    int MULTEQ_Id = SyntaxTokenIds.MULTEQ;
    int DIVEQ_Id = SyntaxTokenIds.DIVEQ;
    int PERCEQ_Id = SyntaxTokenIds.PERCEQ;
    int PLUSEQ_Id = SyntaxTokenIds.PLUSEQ;
    int MINUSEQ_Id = SyntaxTokenIds.MINUSEQ;
    int NOT_IN_Id = SyntaxTokenIds.NOT_IN;
    int NOT_IS_Id = SyntaxTokenIds.NOT_IS;
    int HASH_Id = SyntaxTokenIds.HASH;
    int AT_Id = SyntaxTokenIds.AT;
    int COMMA_Id = SyntaxTokenIds.COMMA;
    int EOL_OR_SEMICOLON_Id = SyntaxTokenIds.EOL_OR_SEMICOLON;
    int FILE_KEYWORD_Id = SyntaxTokenIds.FILE_KEYWORD;
    int FIELD_KEYWORD_Id = SyntaxTokenIds.FIELD_KEYWORD;
    int PROPERTY_KEYWORD_Id = SyntaxTokenIds.PROPERTY_KEYWORD;
    int RECEIVER_KEYWORD_Id = SyntaxTokenIds.RECEIVER_KEYWORD;
    int PARAM_KEYWORD_Id = SyntaxTokenIds.PARAM_KEYWORD;
    int SETPARAM_KEYWORD_Id = SyntaxTokenIds.SETPARAM_KEYWORD;
    int DELEGATE_KEYWORD_Id = SyntaxTokenIds.DELEGATE_KEYWORD;
    int IMPORT_KEYWORD_Id = SyntaxTokenIds.IMPORT_KEYWORD;
    int WHERE_KEYWORD_Id = SyntaxTokenIds.WHERE_KEYWORD;
    int BY_KEYWORD_Id = SyntaxTokenIds.BY_KEYWORD;
    int GET_KEYWORD_Id = SyntaxTokenIds.GET_KEYWORD;
    int SET_KEYWORD_Id = SyntaxTokenIds.SET_KEYWORD;
    int CONSTRUCTOR_KEYWORD_Id = SyntaxTokenIds.CONSTRUCTOR_KEYWORD;
    int INIT_KEYWORD_Id = SyntaxTokenIds.INIT_KEYWORD;
    int CONTEXT_KEYWORD_Id = SyntaxTokenIds.CONTEXT_KEYWORD;
    int CATCH_KEYWORD_Id = SyntaxTokenIds.CATCH_KEYWORD;
    int DYNAMIC_KEYWORD_Id = SyntaxTokenIds.DYNAMIC_KEYWORD;
    int FINALLY_KEYWORD_Id = SyntaxTokenIds.FINALLY_KEYWORD;
    int ABSTRACT_KEYWORD_Id = SyntaxTokenIds.ABSTRACT_MODIFIER;
    int ENUM_KEYWORD_Id = SyntaxTokenIds.ENUM_MODIFIER;
    int CONTRACT_KEYWORD_Id = SyntaxTokenIds.CONTRACT_MODIFIER;
    int OPEN_KEYWORD_Id = SyntaxTokenIds.OPEN_MODIFIER;
    int INNER_KEYWORD_Id = SyntaxTokenIds.INNER_MODIFIER;
    int OVERRIDE_KEYWORD_Id = SyntaxTokenIds.OVERRIDE_MODIFIER;
    int PRIVATE_KEYWORD_Id = SyntaxTokenIds.PRIVATE_MODIFIER;
    int PUBLIC_KEYWORD_Id = SyntaxTokenIds.PUBLIC_MODIFIER;
    int INTERNAL_KEYWORD_Id = SyntaxTokenIds.INTERNAL_MODIFIER;
    int PROTECTED_KEYWORD_Id = SyntaxTokenIds.PROTECTED_MODIFIER;
    int OUT_KEYWORD_Id = SyntaxTokenIds.OUT_MODIFIER;
    int VARARG_KEYWORD_Id = SyntaxTokenIds.VARARG_MODIFIER;
    int REIFIED_KEYWORD_Id = SyntaxTokenIds.REIFIED_MODIFIER;
    int COMPANION_KEYWORD_Id = SyntaxTokenIds.COMPANION_MODIFIER;
    int SEALED_KEYWORD_Id = SyntaxTokenIds.SEALED_MODIFIER;
    int FINAL_KEYWORD_Id = SyntaxTokenIds.FINAL_MODIFIER;
    int LATEINIT_KEYWORD_Id = SyntaxTokenIds.LATEINIT_MODIFIER;
    int DATA_KEYWORD_Id = SyntaxTokenIds.DATA_MODIFIER;
    int VALUE_KEYWORD_Id = SyntaxTokenIds.VALUE_MODIFIER;
    int INLINE_KEYWORD_Id = SyntaxTokenIds.INLINE_MODIFIER;
    int NOINLINE_KEYWORD_Id = SyntaxTokenIds.NOINLINE_MODIFIER;
    int TAILREC_KEYWORD_Id = SyntaxTokenIds.TAILREC_MODIFIER;
    int EXTERNAL_KEYWORD_Id = SyntaxTokenIds.EXTERNAL_MODIFIER;
    int ANNOTATION_KEYWORD_Id = SyntaxTokenIds.ANNOTATION_MODIFIER;
    int CROSSINLINE_KEYWORD_Id = SyntaxTokenIds.CROSSINLINE_MODIFIER;
    int OPERATOR_KEYWORD_Id = SyntaxTokenIds.OPERATOR_MODIFIER;
    int INFIX_KEYWORD_Id = SyntaxTokenIds.INFIX_MODIFIER;
    int CONST_KEYWORD_Id = SyntaxTokenIds.CONST_MODIFIER;
    int SUSPEND_KEYWORD_Id = SyntaxTokenIds.SUSPEND_MODIFIER;
    int EXPECT_KEYWORD_Id = SyntaxTokenIds.EXPECT_MODIFIER;
    int ACTUAL_KEYWORD_Id = SyntaxTokenIds.ACTUAL_MODIFIER;
    int INTERPOLATION_PREFIX_Id = SyntaxTokenIds.INTERPOLATION_PREFIX;
    int ALL_KEYWORD_Id = SyntaxTokenIds.ALL_KEYWORD;
    int ERROR_KEYWORD_Id = SyntaxTokenIds.ERROR_MODIFIER;
    int OR_Id = SyntaxTokenIds.OR;
    int ERROR_SAFE_ACCESS_Id = SyntaxTokenIds.ERROR_SAFE_ACCESS;

    @NotNull KtToken EOF   = new KtToken("EOF", SyntaxTokenIds.EOF);

    @NotNull KtToken RESERVED    = new KtToken("RESERVED", SyntaxTokenIds.RESERVED);

    @NotNull KtToken BLOCK_COMMENT     = new KtToken("BLOCK_COMMENT", SyntaxTokenIds.BLOCK_COMMENT);
    @NotNull KtToken EOL_COMMENT       = new KtToken("EOL_COMMENT", SyntaxTokenIds.EOL_COMMENT);
    @NotNull KtToken SHEBANG_COMMENT   = new KtToken("SHEBANG_COMMENT", SyntaxTokenIds.SHEBANG_COMMENT);

    @NotNull IElementType DOC_COMMENT   = KDocTokens.KDOC;

    @NotNull IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    @NotNull KtToken INTEGER_LITERAL    = new KtToken("INTEGER_LITERAL", SyntaxTokenIds.INTEGER_LITERAL);
    @NotNull KtToken FLOAT_LITERAL      = new KtToken("FLOAT_CONSTANT", SyntaxTokenIds.FLOAT_LITERAL);
    @NotNull KtToken CHARACTER_LITERAL  = new KtToken("CHARACTER_LITERAL", SyntaxTokenIds.CHARACTER_LITERAL);

    @NotNull KtToken INTERPOLATION_PREFIX = new KtToken("INTERPOLATION_PREFIX", SyntaxTokenIds.INTERPOLATION_PREFIX);
    @NotNull KtToken CLOSING_QUOTE = new KtToken("CLOSING_QUOTE", SyntaxTokenIds.CLOSING_QUOTE);
    @NotNull KtToken OPEN_QUOTE = new KtToken("OPEN_QUOTE", SyntaxTokenIds.OPEN_QUOTE);
    @NotNull KtToken REGULAR_STRING_PART = new KtToken("REGULAR_STRING_PART", SyntaxTokenIds.REGULAR_STRING_PART);
    @NotNull KtToken ESCAPE_SEQUENCE = new KtToken("ESCAPE_SEQUENCE", SyntaxTokenIds.ESCAPE_SEQUENCE);
    @NotNull KtToken SHORT_TEMPLATE_ENTRY_START = new KtToken("SHORT_TEMPLATE_ENTRY_START", SyntaxTokenIds.SHORT_TEMPLATE_ENTRY_START);
    @NotNull KtToken LONG_TEMPLATE_ENTRY_START = new KtToken("LONG_TEMPLATE_ENTRY_START", SyntaxTokenIds.LONG_TEMPLATE_ENTRY_START);
    @NotNull KtToken LONG_TEMPLATE_ENTRY_END = new KtToken("LONG_TEMPLATE_ENTRY_END", SyntaxTokenIds.LONG_TEMPLATE_ENTRY_END);
    @NotNull KtToken DANGLING_NEWLINE = new KtToken("DANGLING_NEWLINE", SyntaxTokenIds.DANGLING_NEWLINE);

    @NotNull KtKeywordToken PACKAGE_KEYWORD          = KtKeywordToken.keyword("package", SyntaxTokenIds.PACKAGE_KEYWORD);
    @NotNull KtKeywordToken AS_KEYWORD               = KtKeywordToken.keyword("as", SyntaxTokenIds.AS_KEYWORD);
    @NotNull KtKeywordToken TYPE_ALIAS_KEYWORD       = KtKeywordToken.keyword("typealias", SyntaxTokenIds.TYPE_ALIAS_KEYWORD);
    @NotNull KtKeywordToken CLASS_KEYWORD            = KtKeywordToken.keyword("class", SyntaxTokenIds.CLASS_KEYWORD);
    @NotNull KtKeywordToken THIS_KEYWORD             = KtKeywordToken.keyword("this", SyntaxTokenIds.THIS_KEYWORD);
    @NotNull KtKeywordToken SUPER_KEYWORD            = KtKeywordToken.keyword("super", SyntaxTokenIds.SUPER_KEYWORD);
    @NotNull KtKeywordToken VAL_KEYWORD              = KtKeywordToken.keyword("val", SyntaxTokenIds.VAL_KEYWORD);
    @NotNull KtKeywordToken VAR_KEYWORD              = KtKeywordToken.keyword("var", SyntaxTokenIds.VAR_KEYWORD);
    @NotNull KtModifierKeywordToken FUN_KEYWORD      = KtModifierKeywordToken.keywordModifier("fun", SyntaxTokenIds.FUN_MODIFIER);
    @NotNull KtKeywordToken FOR_KEYWORD              = KtKeywordToken.keyword("for", SyntaxTokenIds.FOR_KEYWORD);
    @NotNull KtKeywordToken NULL_KEYWORD             = KtKeywordToken.keyword("null", SyntaxTokenIds.NULL_KEYWORD);
    @NotNull KtKeywordToken TRUE_KEYWORD             = KtKeywordToken.keyword("true", SyntaxTokenIds.TRUE_KEYWORD);
    @NotNull KtKeywordToken FALSE_KEYWORD            = KtKeywordToken.keyword("false", SyntaxTokenIds.FALSE_KEYWORD);
    @NotNull KtKeywordToken IS_KEYWORD               = KtKeywordToken.keyword("is", SyntaxTokenIds.IS_KEYWORD);
    @NotNull KtModifierKeywordToken IN_KEYWORD       = KtModifierKeywordToken.keywordModifier("in", SyntaxTokenIds.IN_MODIFIER);
    @NotNull KtKeywordToken THROW_KEYWORD            = KtKeywordToken.keyword("throw", SyntaxTokenIds.THROW_KEYWORD);
    @NotNull KtKeywordToken RETURN_KEYWORD           = KtKeywordToken.keyword("return", SyntaxTokenIds.RETURN_KEYWORD);
    @NotNull KtKeywordToken BREAK_KEYWORD            = KtKeywordToken.keyword("break", SyntaxTokenIds.BREAK_KEYWORD);
    @NotNull KtKeywordToken CONTINUE_KEYWORD         = KtKeywordToken.keyword("continue", SyntaxTokenIds.CONTINUE_KEYWORD);
    @NotNull KtKeywordToken OBJECT_KEYWORD           = KtKeywordToken.keyword("object", SyntaxTokenIds.OBJECT_KEYWORD);
    @NotNull KtKeywordToken IF_KEYWORD               = KtKeywordToken.keyword("if", SyntaxTokenIds.IF_KEYWORD);
    @NotNull KtKeywordToken TRY_KEYWORD              = KtKeywordToken.keyword("try", SyntaxTokenIds.TRY_KEYWORD);
    @NotNull KtKeywordToken ELSE_KEYWORD             = KtKeywordToken.keyword("else", SyntaxTokenIds.ELSE_KEYWORD);
    @NotNull KtKeywordToken WHILE_KEYWORD            = KtKeywordToken.keyword("while", SyntaxTokenIds.WHILE_KEYWORD);
    @NotNull KtKeywordToken DO_KEYWORD               = KtKeywordToken.keyword("do", SyntaxTokenIds.DO_KEYWORD);
    @NotNull KtKeywordToken WHEN_KEYWORD             = KtKeywordToken.keyword("when", SyntaxTokenIds.WHEN_KEYWORD);
    @NotNull KtKeywordToken INTERFACE_KEYWORD        = KtKeywordToken.keyword("interface", SyntaxTokenIds.INTERFACE_KEYWORD);

    // Reserved for future use:
    @NotNull KtKeywordToken TYPEOF_KEYWORD           = KtKeywordToken.keyword("typeof", SyntaxTokenIds.TYPEOF_KEYWORD);

    @NotNull KtToken AS_SAFE = KtKeywordToken.keyword("AS_SAFE", "as?", SyntaxTokenIds.AS_SAFE);

    @NotNull KtToken IDENTIFIER = new KtToken("IDENTIFIER", SyntaxTokenIds.IDENTIFIER);

    @NotNull KtToken FIELD_IDENTIFIER = new KtToken("FIELD_IDENTIFIER", SyntaxTokenIds.FIELD_IDENTIFIER);
    @NotNull KtSingleValueToken LBRACKET    = new KtSingleValueToken("LBRACKET", "[", SyntaxTokenIds.LBRACKET);
    @NotNull KtSingleValueToken RBRACKET    = new KtSingleValueToken("RBRACKET", "]", SyntaxTokenIds.RBRACKET);
    @NotNull KtSingleValueToken LBRACE      = new KtSingleValueToken("LBRACE", "{", SyntaxTokenIds.LBRACE);
    @NotNull KtSingleValueToken RBRACE      = new KtSingleValueToken("RBRACE", "}", SyntaxTokenIds.RBRACE);
    @NotNull KtSingleValueToken LPAR        = new KtSingleValueToken("LPAR", "(", SyntaxTokenIds.LPAR);
    @NotNull KtSingleValueToken RPAR        = new KtSingleValueToken("RPAR", ")", SyntaxTokenIds.RPAR);
    @NotNull KtSingleValueToken DOT         = new KtSingleValueToken("DOT", ".", SyntaxTokenIds.DOT);
    @NotNull KtSingleValueToken PLUSPLUS    = new KtSingleValueToken("PLUSPLUS", "++", SyntaxTokenIds.PLUSPLUS);
    @NotNull KtSingleValueToken MINUSMINUS  = new KtSingleValueToken("MINUSMINUS", "--", SyntaxTokenIds.MINUSMINUS);
    @NotNull KtSingleValueToken MUL         = new KtSingleValueToken("MUL", "*", SyntaxTokenIds.MUL);
    @NotNull KtSingleValueToken PLUS        = new KtSingleValueToken("PLUS", "+", SyntaxTokenIds.PLUS);
    @NotNull KtSingleValueToken MINUS       = new KtSingleValueToken("MINUS", "-", SyntaxTokenIds.MINUS);
    @NotNull KtSingleValueToken EXCL        = new KtSingleValueToken("EXCL", "!", SyntaxTokenIds.EXCL);
    @NotNull KtSingleValueToken DIV         = new KtSingleValueToken("DIV", "/", SyntaxTokenIds.DIV);
    @NotNull KtSingleValueToken PERC        = new KtSingleValueToken("PERC", "%", SyntaxTokenIds.PERC);
    @NotNull KtSingleValueToken LT          = new KtSingleValueToken("LT", "<", SyntaxTokenIds.LT);
    @NotNull KtSingleValueToken GT          = new KtSingleValueToken("GT", ">", SyntaxTokenIds.GT);
    @NotNull KtSingleValueToken LTEQ        = new KtSingleValueToken("LTEQ", "<=", SyntaxTokenIds.LTEQ);
    @NotNull KtSingleValueToken GTEQ        = new KtSingleValueToken("GTEQ", ">=", SyntaxTokenIds.GTEQ);
    @NotNull KtSingleValueToken EQEQEQ      = new KtSingleValueToken("EQEQEQ", "===", SyntaxTokenIds.EQEQEQ);
    @NotNull KtSingleValueToken ARROW       = new KtSingleValueToken("ARROW", "->", SyntaxTokenIds.ARROW);
    @NotNull KtSingleValueToken DOUBLE_ARROW       = new KtSingleValueToken("DOUBLE_ARROW", "=>", SyntaxTokenIds.DOUBLE_ARROW);
    @NotNull KtSingleValueToken EXCLEQEQEQ  = new KtSingleValueToken("EXCLEQEQEQ", "!==", SyntaxTokenIds.EXCLEQEQEQ);
    @NotNull KtSingleValueToken EQEQ        = new KtSingleValueToken("EQEQ", "==", SyntaxTokenIds.EQEQ);
    @NotNull KtSingleValueToken EXCLEQ      = new KtSingleValueToken("EXCLEQ", "!=", SyntaxTokenIds.EXCLEQ);
    @NotNull KtSingleValueToken EXCLEXCL    = new KtSingleValueToken("EXCLEXCL", "!!", SyntaxTokenIds.EXCLEXCL);
    @NotNull KtSingleValueToken ANDAND      = new KtSingleValueToken("ANDAND", "&&", SyntaxTokenIds.ANDAND);
    @NotNull KtSingleValueToken AND         = new KtSingleValueToken("AND", "&", SyntaxTokenIds.AND);
    @NotNull KtSingleValueToken OROR        = new KtSingleValueToken("OROR", "||", SyntaxTokenIds.OROR);
    @NotNull KtSingleValueToken OR          = new KtSingleValueToken("OR", "|", SyntaxTokenIds.OR);
    @NotNull KtSingleValueToken SAFE_ACCESS = new KtSingleValueToken("SAFE_ACCESS", "?.", SyntaxTokenIds.SAFE_ACCESS);
    @NotNull KtSingleValueToken ERROR_SAFE_ACCESS = new KtSingleValueToken("ERROR_SAFE_ACCESS", "|.", SyntaxTokenIds.ERROR_SAFE_ACCESS);
    @NotNull KtSingleValueToken ELVIS       = new KtSingleValueToken("ELVIS", "?:", SyntaxTokenIds.ELVIS);
    @NotNull KtSingleValueToken QUEST       = new KtSingleValueToken("QUEST", "?", SyntaxTokenIds.QUEST);
    @NotNull KtSingleValueToken COLONCOLON  = new KtSingleValueToken("COLONCOLON", "::", SyntaxTokenIds.COLONCOLON);
    @NotNull KtSingleValueToken COLON       = new KtSingleValueToken("COLON", ":", SyntaxTokenIds.COLON);
    @NotNull KtSingleValueToken SEMICOLON   = new KtSingleValueToken("SEMICOLON", ";", SyntaxTokenIds.SEMICOLON);
    @NotNull KtSingleValueToken DOUBLE_SEMICOLON   = new KtSingleValueToken("DOUBLE_SEMICOLON", ";;", SyntaxTokenIds.DOUBLE_SEMICOLON);
    @NotNull KtSingleValueToken RANGE       = new KtSingleValueToken("RANGE", "..", SyntaxTokenIds.RANGE);
    @NotNull KtSingleValueToken RANGE_UNTIL       = new KtSingleValueToken("RANGE_UNTIL", "..<", SyntaxTokenIds.RANGE_UNTIL);
    @NotNull KtSingleValueToken EQ          = new KtSingleValueToken("EQ", "=", SyntaxTokenIds.EQ);
    @NotNull KtSingleValueToken MULTEQ      = new KtSingleValueToken("MULTEQ", "*=", SyntaxTokenIds.MULTEQ);
    @NotNull KtSingleValueToken DIVEQ       = new KtSingleValueToken("DIVEQ", "/=", SyntaxTokenIds.DIVEQ);
    @NotNull KtSingleValueToken PERCEQ      = new KtSingleValueToken("PERCEQ", "%=", SyntaxTokenIds.PERCEQ);
    @NotNull KtSingleValueToken PLUSEQ      = new KtSingleValueToken("PLUSEQ", "+=", SyntaxTokenIds.PLUSEQ);
    @NotNull KtSingleValueToken MINUSEQ     = new KtSingleValueToken("MINUSEQ", "-=", SyntaxTokenIds.MINUSEQ);
    @NotNull KtKeywordToken NOT_IN      = KtKeywordToken.keyword("NOT_IN", "!in", SyntaxTokenIds.NOT_IN);
    @NotNull KtKeywordToken NOT_IS      = KtKeywordToken.keyword("NOT_IS", "!is", SyntaxTokenIds.NOT_IS);
    @NotNull KtSingleValueToken HASH        = new KtSingleValueToken("HASH", "#", SyntaxTokenIds.HASH);
    @NotNull KtSingleValueToken AT          = new KtSingleValueToken("AT", "@", SyntaxTokenIds.AT);

    @NotNull KtSingleValueToken COMMA       = new KtSingleValueToken("COMMA", ",", SyntaxTokenIds.COMMA);

    @NotNull KtToken EOL_OR_SEMICOLON   = new KtToken("EOL_OR_SEMICOLON", SyntaxTokenIds.EOL_OR_SEMICOLON);
    @NotNull KtKeywordToken ALL_KEYWORD    = KtKeywordToken.softKeyword("all", SyntaxTokenIds.ALL_KEYWORD);
    @NotNull KtKeywordToken FILE_KEYWORD    = KtKeywordToken.softKeyword("file", SyntaxTokenIds.FILE_KEYWORD);
    @NotNull KtKeywordToken FIELD_KEYWORD     = KtKeywordToken.softKeyword("field", SyntaxTokenIds.FIELD_KEYWORD);
    @NotNull KtKeywordToken PROPERTY_KEYWORD     = KtKeywordToken.softKeyword("property", SyntaxTokenIds.PROPERTY_KEYWORD);
    @NotNull KtKeywordToken RECEIVER_KEYWORD     = KtKeywordToken.softKeyword("receiver", SyntaxTokenIds.RECEIVER_KEYWORD);
    @NotNull KtKeywordToken PARAM_KEYWORD     = KtKeywordToken.softKeyword("param", SyntaxTokenIds.PARAM_KEYWORD);
    @NotNull KtKeywordToken SETPARAM_KEYWORD  = KtKeywordToken.softKeyword("setparam", SyntaxTokenIds.SETPARAM_KEYWORD);
    @NotNull KtKeywordToken DELEGATE_KEYWORD  = KtKeywordToken.softKeyword("delegate", SyntaxTokenIds.DELEGATE_KEYWORD);
    @NotNull KtKeywordToken IMPORT_KEYWORD    = KtKeywordToken.softKeyword("import", SyntaxTokenIds.IMPORT_KEYWORD);
    @NotNull KtKeywordToken WHERE_KEYWORD     = KtKeywordToken.softKeyword("where", SyntaxTokenIds.WHERE_KEYWORD);
    @NotNull KtKeywordToken BY_KEYWORD        = KtKeywordToken.softKeyword("by", SyntaxTokenIds.BY_KEYWORD);
    @NotNull KtKeywordToken GET_KEYWORD       = KtKeywordToken.softKeyword("get", SyntaxTokenIds.GET_KEYWORD);
    @NotNull KtKeywordToken SET_KEYWORD       = KtKeywordToken.softKeyword("set", SyntaxTokenIds.SET_KEYWORD);
    @NotNull KtKeywordToken CONSTRUCTOR_KEYWORD = KtKeywordToken.softKeyword("constructor", SyntaxTokenIds.CONSTRUCTOR_KEYWORD);
    @NotNull KtKeywordToken INIT_KEYWORD        = KtKeywordToken.softKeyword("init", SyntaxTokenIds.INIT_KEYWORD);
    @NotNull KtKeywordToken CONTEXT_KEYWORD     = KtKeywordToken.softKeyword("context", SyntaxTokenIds.CONTEXT_KEYWORD);
    @NotNull KtKeywordToken CATCH_KEYWORD     = KtKeywordToken.softKeyword("catch", SyntaxTokenIds.CATCH_KEYWORD);
    @NotNull KtKeywordToken DYNAMIC_KEYWORD   = KtKeywordToken.softKeyword("dynamic", SyntaxTokenIds.DYNAMIC_KEYWORD);
    @NotNull KtKeywordToken FINALLY_KEYWORD   = KtKeywordToken.softKeyword("finally", SyntaxTokenIds.FINALLY_KEYWORD);

    @NotNull KtModifierKeywordToken ABSTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("abstract", SyntaxTokenIds.ABSTRACT_MODIFIER);
    @NotNull KtModifierKeywordToken ENUM_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("enum", SyntaxTokenIds.ENUM_MODIFIER);
    @NotNull KtModifierKeywordToken CONTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("contract", SyntaxTokenIds.CONTRACT_MODIFIER);
    @NotNull KtModifierKeywordToken OPEN_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("open", SyntaxTokenIds.OPEN_MODIFIER);
    @NotNull KtModifierKeywordToken INNER_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("inner", SyntaxTokenIds.INNER_MODIFIER);
    @NotNull KtModifierKeywordToken OVERRIDE_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("override", SyntaxTokenIds.OVERRIDE_MODIFIER);
    @NotNull KtModifierKeywordToken PRIVATE_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("private", SyntaxTokenIds.PRIVATE_MODIFIER);
    @NotNull KtModifierKeywordToken PUBLIC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("public", SyntaxTokenIds.PUBLIC_MODIFIER);
    @NotNull KtModifierKeywordToken INTERNAL_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("internal", SyntaxTokenIds.INTERNAL_MODIFIER);
    @NotNull KtModifierKeywordToken PROTECTED_KEYWORD = KtModifierKeywordToken.softKeywordModifier("protected", SyntaxTokenIds.PROTECTED_MODIFIER);
    @NotNull KtModifierKeywordToken OUT_KEYWORD       = KtModifierKeywordToken.softKeywordModifier("out", SyntaxTokenIds.OUT_MODIFIER);
    @NotNull KtModifierKeywordToken VARARG_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("vararg", SyntaxTokenIds.VARARG_MODIFIER);
    @NotNull KtModifierKeywordToken REIFIED_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("reified", SyntaxTokenIds.REIFIED_MODIFIER);
    @NotNull KtModifierKeywordToken COMPANION_KEYWORD = KtModifierKeywordToken.softKeywordModifier("companion", SyntaxTokenIds.COMPANION_MODIFIER);
    @NotNull KtModifierKeywordToken SEALED_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("sealed", SyntaxTokenIds.SEALED_MODIFIER);
    @NotNull KtModifierKeywordToken ERROR_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("error", SyntaxTokenIds.ERROR_MODIFIER);

    @Deprecated @NotNull KtModifierKeywordToken DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD;

    @NotNull KtModifierKeywordToken FINAL_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("final", SyntaxTokenIds.FINAL_MODIFIER);

    @NotNull KtModifierKeywordToken LATEINIT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("lateinit", SyntaxTokenIds.LATEINIT_MODIFIER);

    @NotNull KtModifierKeywordToken DATA_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("data", SyntaxTokenIds.DATA_MODIFIER);
    @NotNull KtModifierKeywordToken VALUE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("value", SyntaxTokenIds.VALUE_MODIFIER);
    @NotNull KtModifierKeywordToken INLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("inline", SyntaxTokenIds.INLINE_MODIFIER);
    @NotNull KtModifierKeywordToken NOINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("noinline", SyntaxTokenIds.NOINLINE_MODIFIER);
    @NotNull KtModifierKeywordToken TAILREC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("tailrec", SyntaxTokenIds.TAILREC_MODIFIER);
    @NotNull KtModifierKeywordToken EXTERNAL_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("external", SyntaxTokenIds.EXTERNAL_MODIFIER);
    @NotNull KtModifierKeywordToken ANNOTATION_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("annotation", SyntaxTokenIds.ANNOTATION_MODIFIER);
    @NotNull KtModifierKeywordToken CROSSINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("crossinline", SyntaxTokenIds.CROSSINLINE_MODIFIER);
    @NotNull KtModifierKeywordToken OPERATOR_KEYWORD = KtModifierKeywordToken.softKeywordModifier("operator", SyntaxTokenIds.OPERATOR_MODIFIER);
    @NotNull KtModifierKeywordToken INFIX_KEYWORD = KtModifierKeywordToken.softKeywordModifier("infix", SyntaxTokenIds.INFIX_MODIFIER);

    @NotNull KtModifierKeywordToken CONST_KEYWORD = KtModifierKeywordToken.softKeywordModifier("const", SyntaxTokenIds.CONST_MODIFIER);

    @NotNull KtModifierKeywordToken SUSPEND_KEYWORD = KtModifierKeywordToken.softKeywordModifier("suspend", SyntaxTokenIds.SUSPEND_MODIFIER);

    @NotNull KtModifierKeywordToken EXPECT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("expect", SyntaxTokenIds.EXPECT_MODIFIER);
    @NotNull KtModifierKeywordToken ACTUAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("actual", SyntaxTokenIds.ACTUAL_MODIFIER);


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
