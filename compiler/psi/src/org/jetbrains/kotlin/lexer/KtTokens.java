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
import org.jetbrains.kotlin.ElementTypeChecker;
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

    public static final int EOF_INDEX = KDocElementTypes.KDOC_NAME_INDEX + 1;
    public static final int RESERVED_INDEX = EOF_INDEX + 1;
    public static final int BLOCK_COMMENT_INDEX = RESERVED_INDEX + 1;
    public static final int EOL_COMMENT_INDEX = BLOCK_COMMENT_INDEX + 1;
    public static final int SHEBANG_COMMENT_INDEX = EOL_COMMENT_INDEX + 1;
    public static final int INTEGER_LITERAL_INDEX = SHEBANG_COMMENT_INDEX + 1;
    public static final int FLOAT_LITERAL_INDEX = INTEGER_LITERAL_INDEX + 1;
    public static final int CHARACTER_LITERAL_INDEX = FLOAT_LITERAL_INDEX + 1;
    public static final int INTERPOLATION_PREFIX_INDEX = CHARACTER_LITERAL_INDEX + 1;
    public static final int CLOSING_QUOTE_INDEX = INTERPOLATION_PREFIX_INDEX + 1;
    public static final int OPEN_QUOTE_INDEX = CLOSING_QUOTE_INDEX + 1;
    public static final int REGULAR_STRING_PART_INDEX = OPEN_QUOTE_INDEX + 1;
    public static final int ESCAPE_SEQUENCE_INDEX = REGULAR_STRING_PART_INDEX + 1;
    public static final int SHORT_TEMPLATE_ENTRY_START_INDEX = ESCAPE_SEQUENCE_INDEX + 1;
    public static final int LONG_TEMPLATE_ENTRY_START_INDEX = SHORT_TEMPLATE_ENTRY_START_INDEX + 1;
    public static final int LONG_TEMPLATE_ENTRY_END_INDEX = LONG_TEMPLATE_ENTRY_START_INDEX + 1;
    public static final int DANGLING_NEWLINE_INDEX = LONG_TEMPLATE_ENTRY_END_INDEX + 1;
    public static final int PACKAGE_KEYWORD_INDEX = DANGLING_NEWLINE_INDEX + 1;
    public static final int AS_KEYWORD_INDEX = PACKAGE_KEYWORD_INDEX + 1;
    public static final int TYPE_ALIAS_KEYWORD_INDEX = AS_KEYWORD_INDEX + 1;
    public static final int CLASS_KEYWORD_INDEX = TYPE_ALIAS_KEYWORD_INDEX + 1;
    public static final int THIS_KEYWORD_INDEX = CLASS_KEYWORD_INDEX + 1;
    public static final int SUPER_KEYWORD_INDEX = THIS_KEYWORD_INDEX + 1;
    public static final int VAL_KEYWORD_INDEX = SUPER_KEYWORD_INDEX + 1;
    public static final int VAR_KEYWORD_INDEX = VAL_KEYWORD_INDEX + 1;
    public static final int FUN_KEYWORD_INDEX = VAR_KEYWORD_INDEX + 1;
    public static final int FOR_KEYWORD_INDEX = FUN_KEYWORD_INDEX + 1;
    public static final int NULL_KEYWORD_INDEX = FOR_KEYWORD_INDEX + 1;
    public static final int TRUE_KEYWORD_INDEX = NULL_KEYWORD_INDEX + 1;
    public static final int FALSE_KEYWORD_INDEX = TRUE_KEYWORD_INDEX + 1;
    public static final int IS_KEYWORD_INDEX = FALSE_KEYWORD_INDEX + 1;
    public static final int IN_KEYWORD_INDEX = IS_KEYWORD_INDEX + 1;
    public static final int THROW_KEYWORD_INDEX = IN_KEYWORD_INDEX + 1;
    public static final int RETURN_KEYWORD_INDEX = THROW_KEYWORD_INDEX + 1;
    public static final int BREAK_KEYWORD_INDEX = RETURN_KEYWORD_INDEX + 1;
    public static final int CONTINUE_KEYWORD_INDEX = BREAK_KEYWORD_INDEX + 1;
    public static final int OBJECT_KEYWORD_INDEX = CONTINUE_KEYWORD_INDEX + 1;
    public static final int IF_KEYWORD_INDEX = OBJECT_KEYWORD_INDEX + 1;
    public static final int TRY_KEYWORD_INDEX = IF_KEYWORD_INDEX + 1;
    public static final int ELSE_KEYWORD_INDEX = TRY_KEYWORD_INDEX + 1;
    public static final int WHILE_KEYWORD_INDEX = ELSE_KEYWORD_INDEX + 1;
    public static final int DO_KEYWORD_INDEX = WHILE_KEYWORD_INDEX + 1;
    public static final int WHEN_KEYWORD_INDEX = DO_KEYWORD_INDEX + 1;
    public static final int INTERFACE_KEYWORD_INDEX = WHEN_KEYWORD_INDEX + 1;
    public static final int TYPEOF_KEYWORD_INDEX = INTERFACE_KEYWORD_INDEX + 1;
    public static final int AS_SAFE_INDEX = TYPEOF_KEYWORD_INDEX + 1;
    public static final int IDENTIFIER_INDEX = AS_SAFE_INDEX + 1;
    public static final int FIELD_IDENTIFIER_INDEX = IDENTIFIER_INDEX + 1;
    public static final int LBRACKET_INDEX = FIELD_IDENTIFIER_INDEX + 1;
    public static final int RBRACKET_INDEX = LBRACKET_INDEX + 1;
    public static final int LBRACE_INDEX = RBRACKET_INDEX + 1;
    public static final int RBRACE_INDEX = LBRACE_INDEX + 1;
    public static final int LPAR_INDEX = RBRACE_INDEX + 1;
    public static final int RPAR_INDEX = LPAR_INDEX + 1;
    public static final int DOT_INDEX = RPAR_INDEX + 1;
    public static final int PLUSPLUS_INDEX = DOT_INDEX + 1;
    public static final int MINUSMINUS_INDEX = PLUSPLUS_INDEX + 1;
    public static final int MUL_INDEX = MINUSMINUS_INDEX + 1;
    public static final int PLUS_INDEX = MUL_INDEX + 1;
    public static final int MINUS_INDEX = PLUS_INDEX + 1;
    public static final int EXCL_INDEX = MINUS_INDEX + 1;
    public static final int DIV_INDEX = EXCL_INDEX + 1;
    public static final int PERC_INDEX = DIV_INDEX + 1;
    public static final int LT_INDEX = PERC_INDEX + 1;
    public static final int GT_INDEX = LT_INDEX + 1;
    public static final int LTEQ_INDEX = GT_INDEX + 1;
    public static final int GTEQ_INDEX = LTEQ_INDEX + 1;
    public static final int EQEQEQ_INDEX = GTEQ_INDEX + 1;
    public static final int ARROW_INDEX = EQEQEQ_INDEX + 1;
    public static final int DOUBLE_ARROW_INDEX = ARROW_INDEX + 1;
    public static final int EXCLEQEQEQ_INDEX = DOUBLE_ARROW_INDEX + 1;
    public static final int EQEQ_INDEX = EXCLEQEQEQ_INDEX + 1;
    public static final int EXCLEQ_INDEX = EQEQ_INDEX + 1;
    public static final int EXCLEXCL_INDEX = EXCLEQ_INDEX + 1;
    public static final int ANDAND_INDEX = EXCLEXCL_INDEX + 1;
    public static final int AND_INDEX = ANDAND_INDEX + 1;
    public static final int OROR_INDEX = AND_INDEX + 1;
    public static final int SAFE_ACCESS_INDEX = OROR_INDEX + 1;
    public static final int ELVIS_INDEX = SAFE_ACCESS_INDEX + 1;
    public static final int QUEST_INDEX = ELVIS_INDEX + 1;
    public static final int COLONCOLON_INDEX = QUEST_INDEX + 1;
    public static final int COLON_INDEX = COLONCOLON_INDEX + 1;
    public static final int SEMICOLON_INDEX = COLON_INDEX + 1;
    public static final int DOUBLE_SEMICOLON_INDEX = SEMICOLON_INDEX + 1;
    public static final int RANGE_INDEX = DOUBLE_SEMICOLON_INDEX + 1;
    public static final int RANGE_UNTIL_INDEX = RANGE_INDEX + 1;
    public static final int EQ_INDEX = RANGE_UNTIL_INDEX + 1;
    public static final int MULTEQ_INDEX = EQ_INDEX + 1;
    public static final int DIVEQ_INDEX = MULTEQ_INDEX + 1;
    public static final int PERCEQ_INDEX = DIVEQ_INDEX + 1;
    public static final int PLUSEQ_INDEX = PERCEQ_INDEX + 1;
    public static final int MINUSEQ_INDEX = PLUSEQ_INDEX + 1;
    public static final int NOT_IN_INDEX = MINUSEQ_INDEX + 1;
    public static final int NOT_IS_INDEX = NOT_IN_INDEX + 1;
    public static final int HASH_INDEX = NOT_IS_INDEX + 1;
    public static final int AT_INDEX = HASH_INDEX + 1;
    public static final int COMMA_INDEX = AT_INDEX + 1;
    public static final int EOL_OR_SEMICOLON_INDEX = COMMA_INDEX + 1;
    public static final int ALL_KEYWORD_INDEX = EOL_OR_SEMICOLON_INDEX + 1;
    public static final int FILE_KEYWORD_INDEX = ALL_KEYWORD_INDEX + 1;
    public static final int FIELD_KEYWORD_INDEX = FILE_KEYWORD_INDEX + 1;
    public static final int PROPERTY_KEYWORD_INDEX = FIELD_KEYWORD_INDEX + 1;
    public static final int RECEIVER_KEYWORD_INDEX = PROPERTY_KEYWORD_INDEX + 1;
    public static final int PARAM_KEYWORD_INDEX = RECEIVER_KEYWORD_INDEX + 1;
    public static final int SETPARAM_KEYWORD_INDEX = PARAM_KEYWORD_INDEX + 1;
    public static final int DELEGATE_KEYWORD_INDEX = SETPARAM_KEYWORD_INDEX + 1;
    public static final int IMPORT_KEYWORD_INDEX = DELEGATE_KEYWORD_INDEX + 1;
    public static final int WHERE_KEYWORD_INDEX = IMPORT_KEYWORD_INDEX + 1;
    public static final int BY_KEYWORD_INDEX = WHERE_KEYWORD_INDEX + 1;
    public static final int GET_KEYWORD_INDEX = BY_KEYWORD_INDEX + 1;
    public static final int SET_KEYWORD_INDEX = GET_KEYWORD_INDEX + 1;
    public static final int CONSTRUCTOR_KEYWORD_INDEX = SET_KEYWORD_INDEX + 1;
    public static final int INIT_KEYWORD_INDEX = CONSTRUCTOR_KEYWORD_INDEX + 1;
    public static final int CONTEXT_KEYWORD_INDEX = INIT_KEYWORD_INDEX + 1;
    public static final int ABSTRACT_KEYWORD_INDEX = CONTEXT_KEYWORD_INDEX + 1;
    public static final int ENUM_KEYWORD_INDEX = ABSTRACT_KEYWORD_INDEX + 1;
    public static final int CONTRACT_KEYWORD_INDEX = ENUM_KEYWORD_INDEX + 1;
    public static final int OPEN_KEYWORD_INDEX = CONTRACT_KEYWORD_INDEX + 1;
    public static final int INNER_KEYWORD_INDEX = OPEN_KEYWORD_INDEX + 1;
    public static final int OVERRIDE_KEYWORD_INDEX = INNER_KEYWORD_INDEX + 1;
    public static final int PRIVATE_KEYWORD_INDEX = OVERRIDE_KEYWORD_INDEX + 1;
    public static final int PUBLIC_KEYWORD_INDEX = PRIVATE_KEYWORD_INDEX + 1;
    public static final int INTERNAL_KEYWORD_INDEX = PUBLIC_KEYWORD_INDEX + 1;
    public static final int PROTECTED_KEYWORD_INDEX = INTERNAL_KEYWORD_INDEX + 1;
    public static final int CATCH_KEYWORD_INDEX = PROTECTED_KEYWORD_INDEX + 1;
    public static final int OUT_KEYWORD_INDEX = CATCH_KEYWORD_INDEX + 1;
    public static final int VARARG_KEYWORD_INDEX = OUT_KEYWORD_INDEX + 1;
    public static final int REIFIED_KEYWORD_INDEX = VARARG_KEYWORD_INDEX + 1;
    public static final int DYNAMIC_KEYWORD_INDEX = REIFIED_KEYWORD_INDEX + 1;
    public static final int COMPANION_KEYWORD_INDEX = DYNAMIC_KEYWORD_INDEX + 1;
    public static final int SEALED_KEYWORD_INDEX = COMPANION_KEYWORD_INDEX + 1;
    public static final int FINALLY_KEYWORD_INDEX = SEALED_KEYWORD_INDEX + 1;
    public static final int FINAL_KEYWORD_INDEX = FINALLY_KEYWORD_INDEX + 1;
    public static final int LATEINIT_KEYWORD_INDEX = FINAL_KEYWORD_INDEX + 1;
    public static final int DATA_KEYWORD_INDEX = LATEINIT_KEYWORD_INDEX + 1;
    public static final int VALUE_KEYWORD_INDEX = DATA_KEYWORD_INDEX + 1;
    public static final int INLINE_KEYWORD_INDEX = VALUE_KEYWORD_INDEX + 1;
    public static final int NOINLINE_KEYWORD_INDEX = INLINE_KEYWORD_INDEX + 1;
    public static final int TAILREC_KEYWORD_INDEX = NOINLINE_KEYWORD_INDEX + 1;
    public static final int EXTERNAL_KEYWORD_INDEX = TAILREC_KEYWORD_INDEX + 1;
    public static final int ANNOTATION_KEYWORD_INDEX = EXTERNAL_KEYWORD_INDEX + 1;
    public static final int CROSSINLINE_KEYWORD_INDEX = ANNOTATION_KEYWORD_INDEX + 1;
    public static final int OPERATOR_KEYWORD_INDEX = CROSSINLINE_KEYWORD_INDEX + 1;
    public static final int INFIX_KEYWORD_INDEX = OPERATOR_KEYWORD_INDEX + 1;
    public static final int CONST_KEYWORD_INDEX = INFIX_KEYWORD_INDEX + 1;
    public static final int SUSPEND_KEYWORD_INDEX = CONST_KEYWORD_INDEX + 1;
    public static final int EXPECT_KEYWORD_INDEX = SUSPEND_KEYWORD_INDEX + 1;
    public static final int ACTUAL_KEYWORD_INDEX = EXPECT_KEYWORD_INDEX + 1;

    public static final KtToken EOF   = new KtToken("EOF");

    public static final KtToken RESERVED    = new KtToken("RESERVED");

    public static final KtToken BLOCK_COMMENT     = new KtToken("BLOCK_COMMENT");
    public static final KtToken EOL_COMMENT       = new KtToken("EOL_COMMENT");
    public static final KtToken SHEBANG_COMMENT   = new KtToken("SHEBANG_COMMENT");

    public static final IElementType DOC_COMMENT   = KDocTokens.KDOC;

    public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;

    public static final KtToken INTEGER_LITERAL    = new KtToken("INTEGER_LITERAL");
    public static final KtToken FLOAT_LITERAL      = new KtToken("FLOAT_CONSTANT");
    public static final KtToken CHARACTER_LITERAL  = new KtToken("CHARACTER_LITERAL");

    public static final KtToken INTERPOLATION_PREFIX = new KtToken("INTERPOLATION_PREFIX");
    public static final KtToken CLOSING_QUOTE = new KtToken("CLOSING_QUOTE");
    public static final KtToken OPEN_QUOTE = new KtToken("OPEN_QUOTE");
    public static final KtToken REGULAR_STRING_PART = new KtToken("REGULAR_STRING_PART");
    public static final KtToken ESCAPE_SEQUENCE = new KtToken("ESCAPE_SEQUENCE");
    public static final KtToken SHORT_TEMPLATE_ENTRY_START = new KtToken("SHORT_TEMPLATE_ENTRY_START");
    public static final KtToken LONG_TEMPLATE_ENTRY_START = new KtToken("LONG_TEMPLATE_ENTRY_START");
    public static final KtToken LONG_TEMPLATE_ENTRY_END = new KtToken("LONG_TEMPLATE_ENTRY_END");
    public static final KtToken DANGLING_NEWLINE = new KtToken("DANGLING_NEWLINE");

    public static final KtKeywordToken PACKAGE_KEYWORD          = KtKeywordToken.keyword("package");
    public static final KtKeywordToken AS_KEYWORD               = KtKeywordToken.keyword("as");
    public static final KtKeywordToken TYPE_ALIAS_KEYWORD       = KtKeywordToken.keyword("typealias");
    public static final KtKeywordToken CLASS_KEYWORD            = KtKeywordToken.keyword("class");
    public static final KtKeywordToken THIS_KEYWORD             = KtKeywordToken.keyword("this");
    public static final KtKeywordToken SUPER_KEYWORD            = KtKeywordToken.keyword("super");
    public static final KtKeywordToken VAL_KEYWORD              = KtKeywordToken.keyword("val");
    public static final KtKeywordToken VAR_KEYWORD              = KtKeywordToken.keyword("var");
    public static final KtModifierKeywordToken FUN_KEYWORD      = KtModifierKeywordToken.keywordModifier("fun");
    public static final KtKeywordToken FOR_KEYWORD              = KtKeywordToken.keyword("for");
    public static final KtKeywordToken NULL_KEYWORD             = KtKeywordToken.keyword("null");
    public static final KtKeywordToken TRUE_KEYWORD             = KtKeywordToken.keyword("true");
    public static final KtKeywordToken FALSE_KEYWORD            = KtKeywordToken.keyword("false");
    public static final KtKeywordToken IS_KEYWORD               = KtKeywordToken.keyword("is");
    public static final KtModifierKeywordToken IN_KEYWORD       = KtModifierKeywordToken.keywordModifier("in");
    public static final KtKeywordToken THROW_KEYWORD            = KtKeywordToken.keyword("throw");
    public static final KtKeywordToken RETURN_KEYWORD           = KtKeywordToken.keyword("return");
    public static final KtKeywordToken BREAK_KEYWORD            = KtKeywordToken.keyword("break");
    public static final KtKeywordToken CONTINUE_KEYWORD         = KtKeywordToken.keyword("continue");
    public static final KtKeywordToken OBJECT_KEYWORD           = KtKeywordToken.keyword("object");
    public static final KtKeywordToken IF_KEYWORD               = KtKeywordToken.keyword("if");
    public static final KtKeywordToken TRY_KEYWORD              = KtKeywordToken.keyword("try");
    public static final KtKeywordToken ELSE_KEYWORD             = KtKeywordToken.keyword("else");
    public static final KtKeywordToken WHILE_KEYWORD            = KtKeywordToken.keyword("while");
    public static final KtKeywordToken DO_KEYWORD               = KtKeywordToken.keyword("do");
    public static final KtKeywordToken WHEN_KEYWORD             = KtKeywordToken.keyword("when");
    public static final KtKeywordToken INTERFACE_KEYWORD        = KtKeywordToken.keyword("interface");

    // Reserved for future use:
    public static final KtKeywordToken TYPEOF_KEYWORD           = KtKeywordToken.keyword("typeof");

    public static final KtToken AS_SAFE = KtKeywordToken.keyword("AS_SAFE");

    public static final KtToken IDENTIFIER = new KtToken("IDENTIFIER");

    public static final KtToken FIELD_IDENTIFIER = new KtToken("FIELD_IDENTIFIER");
    public static final KtSingleValueToken LBRACKET    = new KtSingleValueToken("LBRACKET", "[");
    public static final KtSingleValueToken RBRACKET    = new KtSingleValueToken("RBRACKET", "]");
    public static final KtSingleValueToken LBRACE      = new KtSingleValueToken("LBRACE", "{");
    public static final KtSingleValueToken RBRACE      = new KtSingleValueToken("RBRACE", "}");
    public static final KtSingleValueToken LPAR        = new KtSingleValueToken("LPAR", "(");
    public static final KtSingleValueToken RPAR        = new KtSingleValueToken("RPAR", ")");
    public static final KtSingleValueToken DOT         = new KtSingleValueToken("DOT", ".");
    public static final KtSingleValueToken PLUSPLUS    = new KtSingleValueToken("PLUSPLUS", "++");
    public static final KtSingleValueToken MINUSMINUS  = new KtSingleValueToken("MINUSMINUS", "--");
    public static final KtSingleValueToken MUL         = new KtSingleValueToken("MUL", "*");
    public static final KtSingleValueToken PLUS        = new KtSingleValueToken("PLUS", "+");
    public static final KtSingleValueToken MINUS       = new KtSingleValueToken("MINUS", "-");
    public static final KtSingleValueToken EXCL        = new KtSingleValueToken("EXCL", "!");
    public static final KtSingleValueToken DIV         = new KtSingleValueToken("DIV", "/");
    public static final KtSingleValueToken PERC        = new KtSingleValueToken("PERC", "%");
    public static final KtSingleValueToken LT          = new KtSingleValueToken("LT", "<");
    public static final KtSingleValueToken GT          = new KtSingleValueToken("GT", ">");
    public static final KtSingleValueToken LTEQ        = new KtSingleValueToken("LTEQ", "<=");
    public static final KtSingleValueToken GTEQ        = new KtSingleValueToken("GTEQ", ">=");
    public static final KtSingleValueToken EQEQEQ      = new KtSingleValueToken("EQEQEQ", "===");
    public static final KtSingleValueToken ARROW       = new KtSingleValueToken("ARROW", "->");
    public static final KtSingleValueToken DOUBLE_ARROW       = new KtSingleValueToken("DOUBLE_ARROW", "=>");
    public static final KtSingleValueToken EXCLEQEQEQ  = new KtSingleValueToken("EXCLEQEQEQ", "!==");
    public static final KtSingleValueToken EQEQ        = new KtSingleValueToken("EQEQ", "==");
    public static final KtSingleValueToken EXCLEQ      = new KtSingleValueToken("EXCLEQ", "!=");
    public static final KtSingleValueToken EXCLEXCL    = new KtSingleValueToken("EXCLEXCL", "!!");
    public static final KtSingleValueToken ANDAND      = new KtSingleValueToken("ANDAND", "&&");
    public static final KtSingleValueToken AND         = new KtSingleValueToken("AND", "&");
    public static final KtSingleValueToken OROR        = new KtSingleValueToken("OROR", "||");
    public static final KtSingleValueToken SAFE_ACCESS = new KtSingleValueToken("SAFE_ACCESS", "?.");
    public static final KtSingleValueToken ELVIS       = new KtSingleValueToken("ELVIS", "?:");
    public static final KtSingleValueToken QUEST       = new KtSingleValueToken("QUEST", "?");
    public static final KtSingleValueToken COLONCOLON  = new KtSingleValueToken("COLONCOLON", "::");
    public static final KtSingleValueToken COLON       = new KtSingleValueToken("COLON", ":");
    public static final KtSingleValueToken SEMICOLON   = new KtSingleValueToken("SEMICOLON", ";");
    public static final KtSingleValueToken DOUBLE_SEMICOLON   = new KtSingleValueToken("DOUBLE_SEMICOLON", ";;");
    public static final KtSingleValueToken RANGE       = new KtSingleValueToken("RANGE", "..");
    public static final KtSingleValueToken RANGE_UNTIL       = new KtSingleValueToken("RANGE_UNTIL", "..<");
    public static final KtSingleValueToken EQ          = new KtSingleValueToken("EQ", "=");
    public static final KtSingleValueToken MULTEQ      = new KtSingleValueToken("MULTEQ", "*=");
    public static final KtSingleValueToken DIVEQ       = new KtSingleValueToken("DIVEQ", "/=");
    public static final KtSingleValueToken PERCEQ      = new KtSingleValueToken("PERCEQ", "%=");
    public static final KtSingleValueToken PLUSEQ      = new KtSingleValueToken("PLUSEQ", "+=");
    public static final KtSingleValueToken MINUSEQ     = new KtSingleValueToken("MINUSEQ", "-=");
    public static final KtKeywordToken NOT_IN      = KtKeywordToken.keyword("NOT_IN", "!in");
    public static final KtKeywordToken NOT_IS      = KtKeywordToken.keyword("NOT_IS", "!is");
    public static final KtSingleValueToken HASH        = new KtSingleValueToken("HASH", "#");
    public static final KtSingleValueToken AT          = new KtSingleValueToken("AT", "@");

    public static final KtSingleValueToken COMMA       = new KtSingleValueToken("COMMA", ",");

    public static final KtToken EOL_OR_SEMICOLON   = new KtToken("EOL_OR_SEMICOLON");
    public static final KtKeywordToken ALL_KEYWORD    = KtKeywordToken.softKeyword("all");
    public static final KtKeywordToken FILE_KEYWORD    = KtKeywordToken.softKeyword("file");
    public static final KtKeywordToken FIELD_KEYWORD     = KtKeywordToken.softKeyword("field");
    public static final KtKeywordToken PROPERTY_KEYWORD     = KtKeywordToken.softKeyword("property");
    public static final KtKeywordToken RECEIVER_KEYWORD     = KtKeywordToken.softKeyword("receiver");
    public static final KtKeywordToken PARAM_KEYWORD     = KtKeywordToken.softKeyword("param");
    public static final KtKeywordToken SETPARAM_KEYWORD  = KtKeywordToken.softKeyword("setparam");
    public static final KtKeywordToken DELEGATE_KEYWORD  = KtKeywordToken.softKeyword("delegate");
    public static final KtKeywordToken IMPORT_KEYWORD    = KtKeywordToken.softKeyword("import");
    public static final KtKeywordToken WHERE_KEYWORD     = KtKeywordToken.softKeyword("where");
    public static final KtKeywordToken BY_KEYWORD        = KtKeywordToken.softKeyword("by");
    public static final KtKeywordToken GET_KEYWORD       = KtKeywordToken.softKeyword("get");
    public static final KtKeywordToken SET_KEYWORD       = KtKeywordToken.softKeyword("set");
    public static final KtKeywordToken CONSTRUCTOR_KEYWORD = KtKeywordToken.softKeyword("constructor");
    public static final KtKeywordToken INIT_KEYWORD        = KtKeywordToken.softKeyword("init");
    public static final KtKeywordToken CONTEXT_KEYWORD     = KtKeywordToken.softKeyword("context");

    public static final KtModifierKeywordToken ABSTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("abstract");
    public static final KtModifierKeywordToken ENUM_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("enum");
    public static final KtModifierKeywordToken CONTRACT_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("contract");
    public static final KtModifierKeywordToken OPEN_KEYWORD      = KtModifierKeywordToken.softKeywordModifier("open");
    public static final KtModifierKeywordToken INNER_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("inner");
    public static final KtModifierKeywordToken OVERRIDE_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("override");
    public static final KtModifierKeywordToken PRIVATE_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("private");
    public static final KtModifierKeywordToken PUBLIC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("public");
    public static final KtModifierKeywordToken INTERNAL_KEYWORD  = KtModifierKeywordToken.softKeywordModifier("internal");
    public static final KtModifierKeywordToken PROTECTED_KEYWORD = KtModifierKeywordToken.softKeywordModifier("protected");
    public static final KtKeywordToken CATCH_KEYWORD     = KtKeywordToken.softKeyword("catch");
    public static final KtModifierKeywordToken OUT_KEYWORD       = KtModifierKeywordToken.softKeywordModifier("out");
    public static final KtModifierKeywordToken VARARG_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("vararg");
    public static final KtModifierKeywordToken REIFIED_KEYWORD   = KtModifierKeywordToken.softKeywordModifier("reified");
    public static final KtKeywordToken DYNAMIC_KEYWORD   = KtKeywordToken.softKeyword("dynamic");
    public static final KtModifierKeywordToken COMPANION_KEYWORD = KtModifierKeywordToken.softKeywordModifier("companion");
    public static final KtModifierKeywordToken SEALED_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("sealed");

    public static final KtModifierKeywordToken DEFAULT_VISIBILITY_KEYWORD = PUBLIC_KEYWORD;

    public static final KtKeywordToken FINALLY_KEYWORD   = KtKeywordToken.softKeyword("finally");
    public static final KtModifierKeywordToken FINAL_KEYWORD     = KtModifierKeywordToken.softKeywordModifier("final");

    public static final KtModifierKeywordToken LATEINIT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("lateinit");

    public static final KtModifierKeywordToken DATA_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("data");
    public static final KtModifierKeywordToken VALUE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("value");
    public static final KtModifierKeywordToken INLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("inline");
    public static final KtModifierKeywordToken NOINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("noinline");
    public static final KtModifierKeywordToken TAILREC_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("tailrec");
    public static final KtModifierKeywordToken EXTERNAL_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("external");
    public static final KtModifierKeywordToken ANNOTATION_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("annotation");
    public static final KtModifierKeywordToken CROSSINLINE_KEYWORD    = KtModifierKeywordToken.softKeywordModifier("crossinline");
    public static final KtModifierKeywordToken OPERATOR_KEYWORD = KtModifierKeywordToken.softKeywordModifier("operator");
    public static final KtModifierKeywordToken INFIX_KEYWORD = KtModifierKeywordToken.softKeywordModifier("infix");

    public static final KtModifierKeywordToken CONST_KEYWORD = KtModifierKeywordToken.softKeywordModifier("const");

    public static final KtModifierKeywordToken SUSPEND_KEYWORD = KtModifierKeywordToken.softKeywordModifier("suspend");

    public static final KtModifierKeywordToken EXPECT_KEYWORD = KtModifierKeywordToken.softKeywordModifier("expect");
    public static final KtModifierKeywordToken ACTUAL_KEYWORD = KtModifierKeywordToken.softKeywordModifier("actual");

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

    static {
        ElementTypeChecker.checkExplicitStaticIndexesMatchImplicit(KtTokens.class);
    }
}
