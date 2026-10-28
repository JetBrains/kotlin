/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.kotlin.KtTokenId;
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens;
import org.jetbrains.kotlin.psi.KtPsiUtil;
import org.jetbrains.annotations.NotNull;

import static org.jetbrains.kotlin.KtNodeTypes.*;

public interface KtTokens {
    @NotNull KtToken EOF   = new KtToken("EOF", KtTokenId.EOF);

    @NotNull KtToken RESERVED    = new KtToken("RESERVED", KtTokenId.RESERVED);

    @NotNull KtToken BLOCK_COMMENT     = new KtToken("BLOCK_COMMENT", KtTokenId.BLOCK_COMMENT);
    @NotNull KtToken EOL_COMMENT       = new KtToken("EOL_COMMENT", KtTokenId.EOL_COMMENT);
    @NotNull KtToken SHEBANG_COMMENT   = new KtToken("SHEBANG_COMMENT", KtTokenId.SHEBANG_COMMENT);

    @NotNull IElementType DOC_COMMENT   = KDocTokens.KDOC;

    @NotNull IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    @NotNull KtToken INTEGER_LITERAL    = new KtToken("INTEGER_LITERAL", KtTokenId.INTEGER_LITERAL);
    @NotNull KtToken FLOAT_LITERAL      = new KtToken("FLOAT_CONSTANT", KtTokenId.FLOAT_LITERAL);
    @NotNull KtToken CHARACTER_LITERAL  = new KtToken("CHARACTER_LITERAL", KtTokenId.CHARACTER_LITERAL);

    @NotNull KtToken INTERPOLATION_PREFIX = new KtToken("INTERPOLATION_PREFIX", KtTokenId.INTERPOLATION_PREFIX);
    @NotNull KtToken CLOSING_QUOTE = new KtToken("CLOSING_QUOTE", KtTokenId.CLOSING_QUOTE);
    @NotNull KtToken OPEN_QUOTE = new KtToken("OPEN_QUOTE", KtTokenId.OPEN_QUOTE);
    @NotNull KtToken REGULAR_STRING_PART = new KtToken("REGULAR_STRING_PART", KtTokenId.REGULAR_STRING_PART);
    @NotNull KtToken ESCAPE_SEQUENCE = new KtToken("ESCAPE_SEQUENCE", KtTokenId.ESCAPE_SEQUENCE);
    @NotNull KtToken SHORT_TEMPLATE_ENTRY_START = new KtToken("SHORT_TEMPLATE_ENTRY_START", KtTokenId.SHORT_TEMPLATE_ENTRY_START);
    @NotNull KtToken LONG_TEMPLATE_ENTRY_START = new KtToken("LONG_TEMPLATE_ENTRY_START", KtTokenId.LONG_TEMPLATE_ENTRY_START);
    @NotNull KtToken LONG_TEMPLATE_ENTRY_END = new KtToken("LONG_TEMPLATE_ENTRY_END", KtTokenId.LONG_TEMPLATE_ENTRY_END);
    @NotNull KtToken DANGLING_NEWLINE = new KtToken("DANGLING_NEWLINE", KtTokenId.DANGLING_NEWLINE);

    @NotNull KtKeywordToken PACKAGE_KEYWORD          = KtKeywordToken.keyword("package", KtTokenId.PACKAGE_KEYWORD);
    @NotNull KtKeywordToken AS_KEYWORD               = KtKeywordToken.keyword("as", KtTokenId.AS_KEYWORD);
    @NotNull KtKeywordToken TYPE_ALIAS_KEYWORD       = KtKeywordToken.keyword("typealias", KtTokenId.TYPE_ALIAS_KEYWORD);
    @NotNull KtKeywordToken CLASS_KEYWORD            = KtKeywordToken.keyword("class", KtTokenId.CLASS_KEYWORD);
    @NotNull KtKeywordToken THIS_KEYWORD             = KtKeywordToken.keyword("this", KtTokenId.THIS_KEYWORD);
    @NotNull KtKeywordToken SUPER_KEYWORD            = KtKeywordToken.keyword("super", KtTokenId.SUPER_KEYWORD);
    @NotNull KtKeywordToken VAL_KEYWORD              = KtKeywordToken.keyword("val", KtTokenId.VAL_KEYWORD);
    @NotNull KtKeywordToken VAR_KEYWORD              = KtKeywordToken.keyword("var", KtTokenId.VAR_KEYWORD);
    @NotNull KtModifierKeywordToken FUN_KEYWORD      = KtModifierKeywordToken.keywordModifier("fun", KtTokenId.FUN_MODIFIER);
    @NotNull KtKeywordToken FOR_KEYWORD              = KtKeywordToken.keyword("for", KtTokenId.FOR_KEYWORD);
    @NotNull KtKeywordToken NULL_KEYWORD             = KtKeywordToken.keyword("null", KtTokenId.NULL_KEYWORD);
    @NotNull KtKeywordToken TRUE_KEYWORD             = KtKeywordToken.keyword("true", KtTokenId.TRUE_KEYWORD);
    @NotNull KtKeywordToken FALSE_KEYWORD            = KtKeywordToken.keyword("false", KtTokenId.FALSE_KEYWORD);
    @NotNull KtKeywordToken IS_KEYWORD               = KtKeywordToken.keyword("is", KtTokenId.IS_KEYWORD);
    @NotNull KtModifierKeywordToken IN_KEYWORD       = KtModifierKeywordToken.keywordModifier("in", KtTokenId.IN_MODIFIER);
    @NotNull KtKeywordToken THROW_KEYWORD            = KtKeywordToken.keyword("throw", KtTokenId.THROW_KEYWORD);
    @NotNull KtKeywordToken RETURN_KEYWORD           = KtKeywordToken.keyword("return", KtTokenId.RETURN_KEYWORD);
    @NotNull KtKeywordToken BREAK_KEYWORD            = KtKeywordToken.keyword("break", KtTokenId.BREAK_KEYWORD);
    @NotNull KtKeywordToken CONTINUE_KEYWORD         = KtKeywordToken.keyword("continue", KtTokenId.CONTINUE_KEYWORD);
    @NotNull KtKeywordToken OBJECT_KEYWORD           = KtKeywordToken.keyword("object", KtTokenId.OBJECT_KEYWORD);
    @NotNull KtKeywordToken IF_KEYWORD               = KtKeywordToken.keyword("if", KtTokenId.IF_KEYWORD);
    @NotNull KtKeywordToken TRY_KEYWORD              = KtKeywordToken.keyword("try", KtTokenId.TRY_KEYWORD);
    @NotNull KtKeywordToken ELSE_KEYWORD             = KtKeywordToken.keyword("else", KtTokenId.ELSE_KEYWORD);
    @NotNull KtKeywordToken WHILE_KEYWORD            = KtKeywordToken.keyword("while", KtTokenId.WHILE_KEYWORD);
    @NotNull KtKeywordToken DO_KEYWORD               = KtKeywordToken.keyword("do", KtTokenId.DO_KEYWORD);
    @NotNull KtKeywordToken WHEN_KEYWORD             = KtKeywordToken.keyword("when", KtTokenId.WHEN_KEYWORD);
    @NotNull KtKeywordToken INTERFACE_KEYWORD        = KtKeywordToken.keyword("interface", KtTokenId.INTERFACE_KEYWORD);

    // Reserved for future use:
    @NotNull KtKeywordToken TYPEOF_KEYWORD           = KtKeywordToken.keyword("typeof", KtTokenId.TYPEOF_KEYWORD);

    @NotNull KtToken AS_SAFE = KtKeywordToken.keyword("AS_SAFE", "as?", KtTokenId.AS_SAFE);

    @NotNull KtToken IDENTIFIER = new KtToken("IDENTIFIER", KtTokenId.IDENTIFIER);

    @NotNull KtToken FIELD_IDENTIFIER = new KtToken("FIELD_IDENTIFIER", KtTokenId.FIELD_IDENTIFIER);
    @NotNull KtSingleValueToken LBRACKET    = new KtSingleValueToken("LBRACKET", "[", KtTokenId.LBRACKET);
    @NotNull KtSingleValueToken RBRACKET    = new KtSingleValueToken("RBRACKET", "]", KtTokenId.RBRACKET);
    @NotNull KtSingleValueToken LBRACE      = new KtSingleValueToken("LBRACE", "{", KtTokenId.LBRACE);
    @NotNull KtSingleValueToken RBRACE      = new KtSingleValueToken("RBRACE", "}", KtTokenId.RBRACE);
    @NotNull KtSingleValueToken LPAR        = new KtSingleValueToken("LPAR", "(", KtTokenId.LPAR);
    @NotNull KtSingleValueToken RPAR        = new KtSingleValueToken("RPAR", ")", KtTokenId.RPAR);
    @NotNull KtSingleValueToken DOT         = new KtSingleValueToken("DOT", ".", KtTokenId.DOT);
    @NotNull KtSingleValueToken PLUSPLUS    = new KtSingleValueToken("PLUSPLUS", "++", KtTokenId.PLUSPLUS);
    @NotNull KtSingleValueToken MINUSMINUS  = new KtSingleValueToken("MINUSMINUS", "--", KtTokenId.MINUSMINUS);
    @NotNull KtSingleValueToken MUL         = new KtSingleValueToken("MUL", "*", KtTokenId.MUL);
    @NotNull KtSingleValueToken PLUS        = new KtSingleValueToken("PLUS", "+", KtTokenId.PLUS);
    @NotNull KtSingleValueToken MINUS       = new KtSingleValueToken("MINUS", "-", KtTokenId.MINUS);
    @NotNull KtSingleValueToken EXCL        = new KtSingleValueToken("EXCL", "!", KtTokenId.EXCL);
    @NotNull KtSingleValueToken DIV         = new KtSingleValueToken("DIV", "/", KtTokenId.DIV);
    @NotNull KtSingleValueToken PERC        = new KtSingleValueToken("PERC", "%", KtTokenId.PERC);
    @NotNull KtSingleValueToken LT          = new KtSingleValueToken("LT", "<", KtTokenId.LT);
    @NotNull KtSingleValueToken GT          = new KtSingleValueToken("GT", ">", KtTokenId.GT);
    @NotNull KtSingleValueToken LTEQ        = new KtSingleValueToken("LTEQ", "<=", KtTokenId.LTEQ);
    @NotNull KtSingleValueToken GTEQ        = new KtSingleValueToken("GTEQ", ">=", KtTokenId.GTEQ);
    @NotNull KtSingleValueToken EQEQEQ      = new KtSingleValueToken("EQEQEQ", "===", KtTokenId.EQEQEQ);
    @NotNull KtSingleValueToken ARROW       = new KtSingleValueToken("ARROW", "->", KtTokenId.ARROW);
    @NotNull KtSingleValueToken DOUBLE_ARROW       = new KtSingleValueToken("DOUBLE_ARROW", "=>", KtTokenId.DOUBLE_ARROW);
    @NotNull KtSingleValueToken EXCLEQEQEQ  = new KtSingleValueToken("EXCLEQEQEQ", "!==", KtTokenId.EXCLEQEQEQ);
    @NotNull KtSingleValueToken EQEQ        = new KtSingleValueToken("EQEQ", "==", KtTokenId.EQEQ);
    @NotNull KtSingleValueToken EXCLEQ      = new KtSingleValueToken("EXCLEQ", "!=", KtTokenId.EXCLEQ);
    @NotNull KtSingleValueToken EXCLEXCL    = new KtSingleValueToken("EXCLEXCL", "!!", KtTokenId.EXCLEXCL);
    @NotNull KtSingleValueToken ANDAND      = new KtSingleValueToken("ANDAND", "&&", KtTokenId.ANDAND);
    @NotNull KtSingleValueToken AND         = new KtSingleValueToken("AND", "&", KtTokenId.AND);
    @NotNull KtSingleValueToken OROR        = new KtSingleValueToken("OROR", "||", KtTokenId.OROR);
    @NotNull KtSingleValueToken OR          = new KtSingleValueToken("OR", "|", KtTokenId.OR);
    @NotNull KtSingleValueToken SAFE_ACCESS = new KtSingleValueToken("SAFE_ACCESS", "?.", KtTokenId.SAFE_ACCESS);
    @NotNull KtSingleValueToken ERROR_SAFE_ACCESS = new KtSingleValueToken("ERROR_SAFE_ACCESS", "|.", KtTokenId.ERROR_SAFE_ACCESS);
    @NotNull KtSingleValueToken ELVIS       = new KtSingleValueToken("ELVIS", "?:", KtTokenId.ELVIS);
    @NotNull KtSingleValueToken QUEST       = new KtSingleValueToken("QUEST", "?", KtTokenId.QUEST);
    @NotNull KtSingleValueToken COLONCOLON  = new KtSingleValueToken("COLONCOLON", "::", KtTokenId.COLONCOLON);
    @NotNull KtSingleValueToken COLON       = new KtSingleValueToken("COLON", ":", KtTokenId.COLON);
    @NotNull KtSingleValueToken SEMICOLON   = new KtSingleValueToken("SEMICOLON", ";", KtTokenId.SEMICOLON);
    @NotNull KtSingleValueToken DOUBLE_SEMICOLON   = new KtSingleValueToken("DOUBLE_SEMICOLON", ";;", KtTokenId.DOUBLE_SEMICOLON);
    @NotNull KtSingleValueToken RANGE       = new KtSingleValueToken("RANGE", "..", KtTokenId.RANGE);
    @NotNull KtSingleValueToken RANGE_UNTIL       = new KtSingleValueToken("RANGE_UNTIL", "..<", KtTokenId.RANGE_UNTIL);
    @NotNull KtSingleValueToken EQ          = new KtSingleValueToken("EQ", "=", KtTokenId.EQ);
    @NotNull KtSingleValueToken MULTEQ      = new KtSingleValueToken("MULTEQ", "*=", KtTokenId.MULTEQ);
    @NotNull KtSingleValueToken DIVEQ       = new KtSingleValueToken("DIVEQ", "/=", KtTokenId.DIVEQ);
    @NotNull KtSingleValueToken PERCEQ      = new KtSingleValueToken("PERCEQ", "%=", KtTokenId.PERCEQ);
    @NotNull KtSingleValueToken PLUSEQ      = new KtSingleValueToken("PLUSEQ", "+=", KtTokenId.PLUSEQ);
    @NotNull KtSingleValueToken MINUSEQ     = new KtSingleValueToken("MINUSEQ", "-=", KtTokenId.MINUSEQ);
    @NotNull KtKeywordToken NOT_IN      = KtKeywordToken.keyword("NOT_IN", "!in", KtTokenId.NOT_IN);
    @NotNull KtKeywordToken NOT_IS      = KtKeywordToken.keyword("NOT_IS", "!is", KtTokenId.NOT_IS);
    @NotNull KtSingleValueToken HASH        = new KtSingleValueToken("HASH", "#", KtTokenId.HASH);
    @NotNull KtSingleValueToken AT          = new KtSingleValueToken("AT", "@", KtTokenId.AT);

    @NotNull KtSingleValueToken COMMA       = new KtSingleValueToken("COMMA", ",", KtTokenId.COMMA);

    @NotNull KtToken EOL_OR_SEMICOLON   = new KtToken("EOL_OR_SEMICOLON", KtTokenId.EOL_OR_SEMICOLON);
    @NotNull KtKeywordToken ALL_KEYWORD    = KtKeywordToken.softKeyword("all", KtTokenId.ALL_KEYWORD);
    @NotNull KtKeywordToken FILE_KEYWORD    = KtKeywordToken.softKeyword("file", KtTokenId.FILE_KEYWORD);
    @NotNull KtKeywordToken FIELD_KEYWORD     = KtKeywordToken.softKeyword("field", KtTokenId.FIELD_KEYWORD);
    @NotNull KtKeywordToken PROPERTY_KEYWORD     = KtKeywordToken.softKeyword("property", KtTokenId.PROPERTY_KEYWORD);
    @NotNull KtKeywordToken RECEIVER_KEYWORD     = KtKeywordToken.softKeyword("receiver", KtTokenId.RECEIVER_KEYWORD);
    @NotNull KtKeywordToken PARAM_KEYWORD     = KtKeywordToken.softKeyword("param", KtTokenId.PARAM_KEYWORD);
    @NotNull KtKeywordToken SETPARAM_KEYWORD  = KtKeywordToken.softKeyword("setparam", KtTokenId.SETPARAM_KEYWORD);
    @NotNull KtKeywordToken DELEGATE_KEYWORD  = KtKeywordToken.softKeyword("delegate", KtTokenId.DELEGATE_KEYWORD);
    @NotNull KtKeywordToken IMPORT_KEYWORD    = KtKeywordToken.softKeyword("import", KtTokenId.IMPORT_KEYWORD);
    @NotNull KtKeywordToken WHERE_KEYWORD     = KtKeywordToken.softKeyword("where", KtTokenId.WHERE_KEYWORD);
    @NotNull KtKeywordToken BY_KEYWORD        = KtKeywordToken.softKeyword("by", KtTokenId.BY_KEYWORD);
    @NotNull KtKeywordToken GET_KEYWORD       = KtKeywordToken.softKeyword("get", KtTokenId.GET_KEYWORD);
    @NotNull KtKeywordToken SET_KEYWORD       = KtKeywordToken.softKeyword("set", KtTokenId.SET_KEYWORD);
    @NotNull KtKeywordToken CONSTRUCTOR_KEYWORD = KtKeywordToken.softKeyword("constructor", KtTokenId.CONSTRUCTOR_KEYWORD);
    @NotNull KtKeywordToken INIT_KEYWORD        = KtKeywordToken.softKeyword("init", KtTokenId.INIT_KEYWORD);
    @NotNull KtKeywordToken CONTEXT_KEYWORD     = KtKeywordToken.softKeyword("context", KtTokenId.CONTEXT_KEYWORD);
    @NotNull KtKeywordToken CATCH_KEYWORD     = KtKeywordToken.softKeyword("catch", KtTokenId.CATCH_KEYWORD);
    @NotNull KtKeywordToken DYNAMIC_KEYWORD   = KtKeywordToken.softKeyword("dynamic", KtTokenId.DYNAMIC_KEYWORD);
    @NotNull KtKeywordToken FINALLY_KEYWORD   = KtKeywordToken.softKeyword("finally", KtTokenId.FINALLY_KEYWORD);

    @NotNull KtModifierKeywordToken ABSTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("abstract", KtTokenId.ABSTRACT_MODIFIER);
    @NotNull KtModifierKeywordToken ENUM_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("enum", KtTokenId.ENUM_MODIFIER);
    @NotNull KtModifierKeywordToken CONTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("contract", KtTokenId.CONTRACT_MODIFIER);
    @NotNull KtModifierKeywordToken OPEN_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("open", KtTokenId.OPEN_MODIFIER);
    @NotNull KtModifierKeywordToken INNER_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("inner", KtTokenId.INNER_MODIFIER);
    @NotNull KtModifierKeywordToken OVERRIDE_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("override", KtTokenId.OVERRIDE_MODIFIER);
    @NotNull KtModifierKeywordToken PRIVATE_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("private", KtTokenId.PRIVATE_MODIFIER);
    @NotNull KtModifierKeywordToken PUBLIC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("public", KtTokenId.PUBLIC_MODIFIER);
    @NotNull KtModifierKeywordToken INTERNAL_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("internal", KtTokenId.INTERNAL_MODIFIER);
    @NotNull KtModifierKeywordToken PROTECTED_KEYWORD = KtModifierKeywordToken.softKeywordModifier("protected", KtTokenId.PROTECTED_MODIFIER);
    @NotNull KtModifierKeywordToken OUT_KEYWORD       = KtModifierKeywordToken.softKeywordModifier("out", KtTokenId.OUT_MODIFIER);
    @NotNull KtModifierKeywordToken VARARG_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("vararg", KtTokenId.VARARG_MODIFIER);
    @NotNull KtModifierKeywordToken REIFIED_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("reified", KtTokenId.REIFIED_MODIFIER);
    @NotNull KtModifierKeywordToken COMPANION_KEYWORD = KtModifierKeywordToken.softKeywordModifier("companion", KtTokenId.COMPANION_MODIFIER);
    @NotNull KtModifierKeywordToken SEALED_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("sealed", KtTokenId.SEALED_MODIFIER);
    @NotNull KtModifierKeywordToken ERROR_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("error", KtTokenId.ERROR_MODIFIER);

    @Deprecated @NotNull KtModifierKeywordToken DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD;

    @NotNull KtModifierKeywordToken FINAL_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("final", KtTokenId.FINAL_MODIFIER);

    @NotNull KtModifierKeywordToken LATEINIT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("lateinit", KtTokenId.LATEINIT_MODIFIER);

    @NotNull KtModifierKeywordToken DATA_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("data", KtTokenId.DATA_MODIFIER);
    @NotNull KtModifierKeywordToken VALUE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("value", KtTokenId.VALUE_MODIFIER);
    @NotNull KtModifierKeywordToken INLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("inline", KtTokenId.INLINE_MODIFIER);
    @NotNull KtModifierKeywordToken NOINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("noinline", KtTokenId.NOINLINE_MODIFIER);
    @NotNull KtModifierKeywordToken TAILREC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("tailrec", KtTokenId.TAILREC_MODIFIER);
    @NotNull KtModifierKeywordToken EXTERNAL_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("external", KtTokenId.EXTERNAL_MODIFIER);
    @NotNull KtModifierKeywordToken ANNOTATION_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("annotation", KtTokenId.ANNOTATION_MODIFIER);
    @NotNull KtModifierKeywordToken CROSSINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("crossinline", KtTokenId.CROSSINLINE_MODIFIER);
    @NotNull KtModifierKeywordToken OPERATOR_KEYWORD = KtModifierKeywordToken.softKeywordModifier("operator", KtTokenId.OPERATOR_MODIFIER);
    @NotNull KtModifierKeywordToken INFIX_KEYWORD = KtModifierKeywordToken.softKeywordModifier("infix", KtTokenId.INFIX_MODIFIER);

    @NotNull KtModifierKeywordToken CONST_KEYWORD = KtModifierKeywordToken.softKeywordModifier("const", KtTokenId.CONST_MODIFIER);

    @NotNull KtModifierKeywordToken SUSPEND_KEYWORD = KtModifierKeywordToken.softKeywordModifier("suspend", KtTokenId.SUSPEND_MODIFIER);

    @NotNull KtModifierKeywordToken EXPECT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("expect", KtTokenId.EXPECT_MODIFIER);
    @NotNull KtModifierKeywordToken ACTUAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("actual", KtTokenId.ACTUAL_MODIFIER);


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
