/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.SyntaxElementTypeSet
import fleet.com.intellij.platform.syntax.element.SyntaxTokenTypes
import fleet.com.intellij.platform.syntax.syntaxElementTypeSetOf
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

@ApiStatus.Experimental
object KtTokens : SyntaxElementTypesWithIds() {
    const val EOF_ID: Int = KDocTokens.KDOC_RPAR_ID + 1
    const val RESERVED_ID: Int = EOF_ID + 1
    const val BLOCK_COMMENT_ID: Int = RESERVED_ID + 1
    const val EOL_COMMENT_ID = BLOCK_COMMENT_ID + 1
    const val SHEBANG_COMMENT_ID = EOL_COMMENT_ID + 1
    const val DOC_COMMENT_ID = SHEBANG_COMMENT_ID + 1

    const val WHITE_SPACE_ID = DOC_COMMENT_ID + 1

    const val INTEGER_LITERAL_ID = WHITE_SPACE_ID + 1
    const val FLOAT_LITERAL_ID = INTEGER_LITERAL_ID + 1
    const val CHARACTER_LITERAL_ID = FLOAT_LITERAL_ID + 1

    const val INTERPOLATION_PREFIX_ID = CHARACTER_LITERAL_ID + 1
    const val CLOSING_QUOTE_ID = INTERPOLATION_PREFIX_ID + 1
    const val OPEN_QUOTE_ID = CLOSING_QUOTE_ID + 1
    const val REGULAR_STRING_PART_ID = OPEN_QUOTE_ID + 1
    const val ESCAPE_SEQUENCE_ID = REGULAR_STRING_PART_ID + 1
    const val SHORT_TEMPLATE_ENTRY_START_ID = ESCAPE_SEQUENCE_ID + 1
    const val LONG_TEMPLATE_ENTRY_START_ID = SHORT_TEMPLATE_ENTRY_START_ID + 1
    const val LONG_TEMPLATE_ENTRY_END_ID = LONG_TEMPLATE_ENTRY_START_ID + 1
    const val DANGLING_NEWLINE_ID = LONG_TEMPLATE_ENTRY_END_ID + 1

    const val PACKAGE_KEYWORD_ID = DANGLING_NEWLINE_ID + 1
    const val AS_KEYWORD_ID = PACKAGE_KEYWORD_ID + 1
    const val TYPE_ALIAS_KEYWORD_ID = AS_KEYWORD_ID + 1
    const val CLASS_KEYWORD_ID = TYPE_ALIAS_KEYWORD_ID + 1
    const val THIS_KEYWORD_ID = CLASS_KEYWORD_ID + 1
    const val SUPER_KEYWORD_ID = THIS_KEYWORD_ID + 1
    const val VAL_KEYWORD_ID = SUPER_KEYWORD_ID + 1
    const val VAR_KEYWORD_ID = VAL_KEYWORD_ID + 1
    const val FUN_KEYWORD_ID = VAR_KEYWORD_ID + 1
    const val FOR_KEYWORD_ID = FUN_KEYWORD_ID + 1
    const val NULL_KEYWORD_ID = FOR_KEYWORD_ID + 1
    const val TRUE_KEYWORD_ID = NULL_KEYWORD_ID + 1
    const val FALSE_KEYWORD_ID = TRUE_KEYWORD_ID + 1
    const val IS_KEYWORD_ID = FALSE_KEYWORD_ID + 1
    const val IN_KEYWORD_ID = IS_KEYWORD_ID + 1
    const val THROW_KEYWORD_ID = IN_KEYWORD_ID + 1
    const val RETURN_KEYWORD_ID = THROW_KEYWORD_ID + 1
    const val BREAK_KEYWORD_ID = RETURN_KEYWORD_ID + 1
    const val CONTINUE_KEYWORD_ID = BREAK_KEYWORD_ID + 1
    const val OBJECT_KEYWORD_ID = CONTINUE_KEYWORD_ID + 1
    const val IF_KEYWORD_ID = OBJECT_KEYWORD_ID + 1
    const val TRY_KEYWORD_ID = IF_KEYWORD_ID + 1
    const val ELSE_KEYWORD_ID = TRY_KEYWORD_ID + 1
    const val WHILE_KEYWORD_ID = ELSE_KEYWORD_ID + 1
    const val DO_KEYWORD_ID = WHILE_KEYWORD_ID + 1
    const val WHEN_KEYWORD_ID = DO_KEYWORD_ID + 1
    const val INTERFACE_KEYWORD_ID = WHEN_KEYWORD_ID + 1

    const val TYPEOF_KEYWORD_ID = INTERFACE_KEYWORD_ID + 1

    const val AS_SAFE_ID = TYPEOF_KEYWORD_ID + 1

    const val IDENTIFIER_ID = AS_SAFE_ID + 1
    const val FIELD_IDENTIFIER_ID = IDENTIFIER_ID + 1
    const val LBRACKET_ID = FIELD_IDENTIFIER_ID + 1
    const val RBRACKET_ID = LBRACKET_ID + 1
    const val LBRACE_ID = RBRACKET_ID + 1
    const val RBRACE_ID = LBRACE_ID + 1
    const val LPAR_ID = RBRACE_ID + 1
    const val RPAR_ID = LPAR_ID + 1
    const val DOT_ID = RPAR_ID + 1
    const val PLUSPLUS_ID = DOT_ID + 1
    const val MINUSMINUS_ID = PLUSPLUS_ID + 1
    const val MUL_ID = MINUSMINUS_ID + 1
    const val PLUS_ID = MUL_ID + 1
    const val MINUS_ID = PLUS_ID + 1
    const val EXCL_ID = MINUS_ID + 1
    const val DIV_ID = EXCL_ID + 1
    const val PERC_ID = DIV_ID + 1
    const val LT_ID = PERC_ID + 1
    const val GT_ID = LT_ID + 1
    const val LTEQ_ID = GT_ID + 1
    const val GTEQ_ID = LTEQ_ID + 1
    const val EQEQEQ_ID = GTEQ_ID + 1
    const val ARROW_ID = EQEQEQ_ID + 1
    const val DOUBLE_ARROW_ID = ARROW_ID + 1
    const val EXCLEQEQEQ_ID = DOUBLE_ARROW_ID + 1
    const val EQEQ_ID = EXCLEQEQEQ_ID + 1
    const val EXCLEQ_ID = EQEQ_ID + 1
    const val EXCLEXCL_ID = EXCLEQ_ID + 1
    const val ANDAND_ID = EXCLEXCL_ID + 1
    const val AND_ID = ANDAND_ID + 1
    const val OROR_ID = AND_ID + 1
    const val SAFE_ACCESS_ID = OROR_ID + 1
    const val ELVIS_ID = SAFE_ACCESS_ID + 1
    const val QUEST_ID = ELVIS_ID + 1
    const val COLONCOLON_ID = QUEST_ID + 1
    const val COLON_ID = COLONCOLON_ID + 1
    const val SEMICOLON_ID = COLON_ID + 1
    const val DOUBLE_SEMICOLON_ID = SEMICOLON_ID + 1
    const val RANGE_ID = DOUBLE_SEMICOLON_ID + 1
    const val RANGE_UNTIL_ID = RANGE_ID + 1
    const val EQ_ID = RANGE_UNTIL_ID + 1
    const val MULTEQ_ID = EQ_ID + 1
    const val DIVEQ_ID = MULTEQ_ID + 1
    const val PERCEQ_ID = DIVEQ_ID + 1
    const val PLUSEQ_ID = PERCEQ_ID + 1
    const val MINUSEQ_ID = PLUSEQ_ID + 1
    const val NOT_IN_ID = MINUSEQ_ID + 1
    const val NOT_IS_ID = NOT_IN_ID + 1
    const val HASH_ID = NOT_IS_ID + 1
    const val AT_ID = HASH_ID + 1
    const val COMMA_ID = AT_ID + 1
    const val EOL_OR_SEMICOLON_ID = COMMA_ID + 1
    const val ALL_KEYWORD_ID = EOL_OR_SEMICOLON_ID + 1
    const val FILE_KEYWORD_ID = ALL_KEYWORD_ID + 1
    const val FIELD_KEYWORD_ID = FILE_KEYWORD_ID + 1
    const val PROPERTY_KEYWORD_ID = FIELD_KEYWORD_ID + 1
    const val RECEIVER_KEYWORD_ID = PROPERTY_KEYWORD_ID + 1
    const val PARAM_KEYWORD_ID = RECEIVER_KEYWORD_ID + 1
    const val SETPARAM_KEYWORD_ID = PARAM_KEYWORD_ID + 1
    const val DELEGATE_KEYWORD_ID = SETPARAM_KEYWORD_ID + 1
    const val IMPORT_KEYWORD_ID = DELEGATE_KEYWORD_ID + 1
    const val WHERE_KEYWORD_ID = IMPORT_KEYWORD_ID + 1
    const val BY_KEYWORD_ID = WHERE_KEYWORD_ID + 1
    const val GET_KEYWORD_ID = BY_KEYWORD_ID + 1
    const val SET_KEYWORD_ID = GET_KEYWORD_ID + 1
    const val CONSTRUCTOR_KEYWORD_ID = SET_KEYWORD_ID + 1
    const val INIT_KEYWORD_ID = CONSTRUCTOR_KEYWORD_ID + 1
    const val CONTEXT_KEYWORD_ID = INIT_KEYWORD_ID + 1
    const val ABSTRACT_KEYWORD_ID = CONTEXT_KEYWORD_ID + 1
    const val ENUM_KEYWORD_ID = ABSTRACT_KEYWORD_ID + 1
    const val CONTRACT_KEYWORD_ID = ENUM_KEYWORD_ID + 1
    const val OPEN_KEYWORD_ID = CONTRACT_KEYWORD_ID + 1
    const val INNER_KEYWORD_ID = OPEN_KEYWORD_ID + 1
    const val OVERRIDE_KEYWORD_ID = INNER_KEYWORD_ID + 1
    const val PRIVATE_KEYWORD_ID = OVERRIDE_KEYWORD_ID + 1
    const val PUBLIC_KEYWORD_ID = PRIVATE_KEYWORD_ID + 1
    const val INTERNAL_KEYWORD_ID = PUBLIC_KEYWORD_ID + 1
    const val PROTECTED_KEYWORD_ID = INTERNAL_KEYWORD_ID + 1
    const val CATCH_KEYWORD_ID = PROTECTED_KEYWORD_ID + 1
    const val OUT_KEYWORD_ID = CATCH_KEYWORD_ID + 1
    const val VARARG_KEYWORD_ID = OUT_KEYWORD_ID + 1
    const val REIFIED_KEYWORD_ID = VARARG_KEYWORD_ID + 1
    const val DYNAMIC_KEYWORD_ID = REIFIED_KEYWORD_ID + 1
    const val COMPANION_KEYWORD_ID = DYNAMIC_KEYWORD_ID + 1
    const val SEALED_KEYWORD_ID = COMPANION_KEYWORD_ID + 1
    const val FINALLY_KEYWORD_ID = SEALED_KEYWORD_ID + 1
    const val FINAL_KEYWORD_ID = FINALLY_KEYWORD_ID + 1
    const val LATEINIT_KEYWORD_ID = FINAL_KEYWORD_ID + 1
    const val DATA_KEYWORD_ID = LATEINIT_KEYWORD_ID + 1
    const val VALUE_KEYWORD_ID = DATA_KEYWORD_ID + 1
    const val INLINE_KEYWORD_ID = VALUE_KEYWORD_ID + 1
    const val NOINLINE_KEYWORD_ID = INLINE_KEYWORD_ID + 1
    const val TAILREC_KEYWORD_ID = NOINLINE_KEYWORD_ID + 1
    const val EXTERNAL_KEYWORD_ID = TAILREC_KEYWORD_ID + 1
    const val ANNOTATION_KEYWORD_ID = EXTERNAL_KEYWORD_ID + 1
    const val CROSSINLINE_KEYWORD_ID = ANNOTATION_KEYWORD_ID + 1
    const val OPERATOR_KEYWORD_ID = CROSSINLINE_KEYWORD_ID + 1
    const val INFIX_KEYWORD_ID = OPERATOR_KEYWORD_ID + 1
    const val CONST_KEYWORD_ID = INFIX_KEYWORD_ID + 1
    const val SUSPEND_KEYWORD_ID = CONST_KEYWORD_ID + 1
    const val EXPECT_KEYWORD_ID = SUSPEND_KEYWORD_ID + 1
    const val ACTUAL_KEYWORD_ID = EXPECT_KEYWORD_ID + 1

    val EOF: SyntaxElementType = registerElementType(EOF_ID, "EOF")

    val RESERVED: SyntaxElementType = registerElementType(RESERVED_ID, "RESERVED")

    val BLOCK_COMMENT: SyntaxElementType = registerElementType(BLOCK_COMMENT_ID, "BLOCK_COMMENT")
    val EOL_COMMENT: SyntaxElementType = registerElementType(EOL_COMMENT_ID, "EOL_COMMENT")
    val SHEBANG_COMMENT: SyntaxElementType = registerElementType(SHEBANG_COMMENT_ID, "SHEBANG_COMMENT")

    val DOC_COMMENT: SyntaxElementType = registerElementType(DOC_COMMENT_ID, "KDoc")

    val WHITE_SPACE: SyntaxElementType = SyntaxTokenTypes.WHITE_SPACE

    val INTEGER_LITERAL: SyntaxElementType = registerElementType(INTEGER_LITERAL_ID, "INTEGER_LITERAL")
    val FLOAT_LITERAL: SyntaxElementType = registerElementType(FLOAT_LITERAL_ID, "FLOAT_CONSTANT")
    val CHARACTER_LITERAL: SyntaxElementType = registerElementType(CHARACTER_LITERAL_ID, "CHARACTER_LITERAL")

    val INTERPOLATION_PREFIX: SyntaxElementType = registerElementType(INTERPOLATION_PREFIX_ID, "INTERPOLATION_PREFIX")
    val CLOSING_QUOTE: SyntaxElementType = registerElementType(CLOSING_QUOTE_ID, "CLOSING_QUOTE")
    val OPEN_QUOTE: SyntaxElementType = registerElementType(OPEN_QUOTE_ID, "OPEN_QUOTE")
    val REGULAR_STRING_PART: SyntaxElementType = registerElementType(REGULAR_STRING_PART_ID, "REGULAR_STRING_PART")
    val ESCAPE_SEQUENCE: SyntaxElementType = registerElementType(ESCAPE_SEQUENCE_ID, "ESCAPE_SEQUENCE")
    val SHORT_TEMPLATE_ENTRY_START: SyntaxElementType = registerElementType(SHORT_TEMPLATE_ENTRY_START_ID, "SHORT_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_START: SyntaxElementType = registerElementType(LONG_TEMPLATE_ENTRY_START_ID, "LONG_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_END: SyntaxElementType = registerElementType(LONG_TEMPLATE_ENTRY_END_ID, "LONG_TEMPLATE_ENTRY_END")
    val DANGLING_NEWLINE: SyntaxElementType = registerElementType(DANGLING_NEWLINE_ID, "DANGLING_NEWLINE")

    val PACKAGE_KEYWORD: SyntaxElementType = registerElementType(PACKAGE_KEYWORD_ID, "package")
    val AS_KEYWORD: SyntaxElementType = registerElementType(AS_KEYWORD_ID, "as")
    val TYPE_ALIAS_KEYWORD: SyntaxElementType = registerElementType(TYPE_ALIAS_KEYWORD_ID, "typealias")
    val CLASS_KEYWORD: SyntaxElementType = registerElementType(CLASS_KEYWORD_ID, "class")
    val THIS_KEYWORD: SyntaxElementType = registerElementType(THIS_KEYWORD_ID, "this")
    val SUPER_KEYWORD: SyntaxElementType = registerElementType(SUPER_KEYWORD_ID, "super")
    val VAL_KEYWORD: SyntaxElementType = registerElementType(VAL_KEYWORD_ID, "val")
    val VAR_KEYWORD: SyntaxElementType = registerElementType(VAR_KEYWORD_ID, "var")
    val FUN_KEYWORD: SyntaxElementType = registerElementType(FUN_KEYWORD_ID, "fun")
    val FOR_KEYWORD: SyntaxElementType = registerElementType(FOR_KEYWORD_ID, "for")
    val NULL_KEYWORD: SyntaxElementType = registerElementType(NULL_KEYWORD_ID, "null")
    val TRUE_KEYWORD: SyntaxElementType = registerElementType(TRUE_KEYWORD_ID, "true")
    val FALSE_KEYWORD: SyntaxElementType = registerElementType(FALSE_KEYWORD_ID, "false")
    val IS_KEYWORD: SyntaxElementType = registerElementType(IS_KEYWORD_ID, "is")
    val IN_KEYWORD: SyntaxElementType = registerElementType(IN_KEYWORD_ID, "in")
    val THROW_KEYWORD: SyntaxElementType = registerElementType(THROW_KEYWORD_ID, "throw")
    val RETURN_KEYWORD: SyntaxElementType = registerElementType(RETURN_KEYWORD_ID, "return")
    val BREAK_KEYWORD: SyntaxElementType = registerElementType(BREAK_KEYWORD_ID, "break")
    val CONTINUE_KEYWORD: SyntaxElementType = registerElementType(CONTINUE_KEYWORD_ID, "continue")
    val OBJECT_KEYWORD: SyntaxElementType = registerElementType(OBJECT_KEYWORD_ID, "object")
    val IF_KEYWORD: SyntaxElementType = registerElementType(IF_KEYWORD_ID, "if")
    val TRY_KEYWORD: SyntaxElementType = registerElementType(TRY_KEYWORD_ID, "try")
    val ELSE_KEYWORD: SyntaxElementType = registerElementType(ELSE_KEYWORD_ID, "else")
    val WHILE_KEYWORD: SyntaxElementType = registerElementType(WHILE_KEYWORD_ID, "while")
    val DO_KEYWORD: SyntaxElementType = registerElementType(DO_KEYWORD_ID, "do")
    val WHEN_KEYWORD: SyntaxElementType = registerElementType(WHEN_KEYWORD_ID, "when")
    val INTERFACE_KEYWORD: SyntaxElementType = registerElementType(INTERFACE_KEYWORD_ID, "interface")

    // Reserved for future use:
    val TYPEOF_KEYWORD: SyntaxElementType = registerElementType(TYPEOF_KEYWORD_ID, "typeof")

    val `AS_SAFE`: SyntaxElementType = registerElementType(AS_SAFE_ID, "AS_SAFE")

    val IDENTIFIER: SyntaxElementType = registerElementType(IDENTIFIER_ID, "IDENTIFIER")

    val FIELD_IDENTIFIER: SyntaxElementType = registerElementType(FIELD_IDENTIFIER_ID, "FIELD_IDENTIFIER")
    val LBRACKET: SyntaxElementType = registerElementType(LBRACKET_ID, "LBRACKET")
    val RBRACKET: SyntaxElementType = registerElementType(RBRACKET_ID, "RBRACKET")
    val LBRACE: SyntaxElementType = registerElementType(LBRACE_ID, "LBRACE")
    val RBRACE: SyntaxElementType = registerElementType(RBRACE_ID, "RBRACE")
    val LPAR: SyntaxElementType = registerElementType(LPAR_ID, "LPAR")
    val RPAR: SyntaxElementType = registerElementType(RPAR_ID, "RPAR")
    val DOT: SyntaxElementType = registerElementType(DOT_ID, "DOT")
    val PLUSPLUS: SyntaxElementType = registerElementType(PLUSPLUS_ID, "PLUSPLUS")
    val MINUSMINUS: SyntaxElementType = registerElementType(MINUSMINUS_ID, "MINUSMINUS")
    val MUL: SyntaxElementType = registerElementType(MUL_ID, "MUL")
    val PLUS: SyntaxElementType = registerElementType(PLUS_ID, "PLUS")
    val MINUS: SyntaxElementType = registerElementType(MINUS_ID, "MINUS")
    val EXCL: SyntaxElementType = registerElementType(EXCL_ID, "EXCL")
    val DIV: SyntaxElementType = registerElementType(DIV_ID, "DIV")
    val PERC: SyntaxElementType = registerElementType(PERC_ID, "PERC")
    val LT: SyntaxElementType = registerElementType(LT_ID, "LT")
    val GT: SyntaxElementType = registerElementType(GT_ID, "GT")
    val LTEQ: SyntaxElementType = registerElementType(LTEQ_ID, "LTEQ")
    val GTEQ: SyntaxElementType = registerElementType(GTEQ_ID, "GTEQ")
    val EQEQEQ: SyntaxElementType = registerElementType(EQEQEQ_ID, "EQEQEQ")
    val ARROW: SyntaxElementType = registerElementType(ARROW_ID, "ARROW")
    val DOUBLE_ARROW: SyntaxElementType = registerElementType(DOUBLE_ARROW_ID, "DOUBLE_ARROW")
    val EXCLEQEQEQ: SyntaxElementType = registerElementType(EXCLEQEQEQ_ID, "EXCLEQEQEQ")
    val EQEQ: SyntaxElementType = registerElementType(EQEQ_ID, "EQEQ")
    val EXCLEQ: SyntaxElementType = registerElementType(EXCLEQ_ID, "EXCLEQ")
    val EXCLEXCL: SyntaxElementType = registerElementType(EXCLEXCL_ID, "EXCLEXCL")
    val ANDAND: SyntaxElementType = registerElementType(ANDAND_ID, "ANDAND")
    val AND: SyntaxElementType = registerElementType(AND_ID, "AND")
    val OROR: SyntaxElementType = registerElementType(OROR_ID, "OROR")
    val SAFE_ACCESS: SyntaxElementType = registerElementType(SAFE_ACCESS_ID, "SAFE_ACCESS")
    val ELVIS: SyntaxElementType = registerElementType(ELVIS_ID, "ELVIS")
    val QUEST: SyntaxElementType = registerElementType(QUEST_ID, "QUEST")
    val COLONCOLON: SyntaxElementType = registerElementType(COLONCOLON_ID, "COLONCOLON")
    val COLON: SyntaxElementType = registerElementType(COLON_ID, "COLON")
    val SEMICOLON: SyntaxElementType = registerElementType(SEMICOLON_ID, "SEMICOLON")
    val DOUBLE_SEMICOLON: SyntaxElementType = registerElementType(DOUBLE_SEMICOLON_ID, "DOUBLE_SEMICOLON")
    val RANGE: SyntaxElementType = registerElementType(RANGE_ID, "RANGE")
    val RANGE_UNTIL: SyntaxElementType = registerElementType(RANGE_UNTIL_ID, "RANGE_UNTIL")
    val EQ: SyntaxElementType = registerElementType(EQ_ID, "EQ")
    val MULTEQ: SyntaxElementType = registerElementType(MULTEQ_ID, "MULTEQ")
    val DIVEQ: SyntaxElementType = registerElementType(DIVEQ_ID, "DIVEQ")
    val PERCEQ: SyntaxElementType = registerElementType(PERCEQ_ID, "PERCEQ")
    val PLUSEQ: SyntaxElementType = registerElementType(PLUSEQ_ID, "PLUSEQ")
    val MINUSEQ: SyntaxElementType = registerElementType(MINUSEQ_ID, "MINUSEQ")
    val NOT_IN: SyntaxElementType = registerElementType(NOT_IN_ID, "NOT_IN")
    val NOT_IS: SyntaxElementType = registerElementType(NOT_IS_ID, "NOT_IS")
    val HASH: SyntaxElementType = registerElementType(HASH_ID, "HASH")
    val AT: SyntaxElementType = registerElementType(AT_ID, "AT")

    val COMMA: SyntaxElementType = registerElementType(COMMA_ID, "COMMA")

    val EOL_OR_SEMICOLON: SyntaxElementType = registerElementType(EOL_OR_SEMICOLON_ID, "EOL_OR_SEMICOLON")
    val ALL_KEYWORD: SyntaxElementType = registerElementType(ALL_KEYWORD_ID, "all")
    val FILE_KEYWORD: SyntaxElementType = registerElementType(FILE_KEYWORD_ID, "file")
    val FIELD_KEYWORD: SyntaxElementType = registerElementType(FIELD_KEYWORD_ID, "field")
    val PROPERTY_KEYWORD: SyntaxElementType = registerElementType(PROPERTY_KEYWORD_ID, "property")
    val RECEIVER_KEYWORD: SyntaxElementType = registerElementType(RECEIVER_KEYWORD_ID, "receiver")
    val PARAM_KEYWORD: SyntaxElementType = registerElementType(PARAM_KEYWORD_ID, "param")
    val SETPARAM_KEYWORD: SyntaxElementType = registerElementType(SETPARAM_KEYWORD_ID, "setparam")
    val DELEGATE_KEYWORD: SyntaxElementType = registerElementType(DELEGATE_KEYWORD_ID, "delegate")
    val IMPORT_KEYWORD: SyntaxElementType = registerElementType(IMPORT_KEYWORD_ID, "import")
    val WHERE_KEYWORD: SyntaxElementType = registerElementType(WHERE_KEYWORD_ID, "where")
    val BY_KEYWORD: SyntaxElementType = registerElementType(BY_KEYWORD_ID, "by")
    val GET_KEYWORD: SyntaxElementType = registerElementType(GET_KEYWORD_ID, "get")
    val SET_KEYWORD: SyntaxElementType = registerElementType(SET_KEYWORD_ID, "set")
    val CONSTRUCTOR_KEYWORD: SyntaxElementType = registerElementType(CONSTRUCTOR_KEYWORD_ID, "constructor")
    val INIT_KEYWORD: SyntaxElementType = registerElementType(INIT_KEYWORD_ID, "init")
    val CONTEXT_KEYWORD: SyntaxElementType = registerElementType(CONTEXT_KEYWORD_ID, "context")

    val ABSTRACT_KEYWORD: SyntaxElementType = registerElementType(ABSTRACT_KEYWORD_ID, "abstract")
    val ENUM_KEYWORD: SyntaxElementType = registerElementType(ENUM_KEYWORD_ID, "enum")
    val CONTRACT_KEYWORD: SyntaxElementType = registerElementType(CONTRACT_KEYWORD_ID, "contract")
    val OPEN_KEYWORD: SyntaxElementType = registerElementType(OPEN_KEYWORD_ID, "open")
    val INNER_KEYWORD: SyntaxElementType = registerElementType(INNER_KEYWORD_ID, "inner")
    val OVERRIDE_KEYWORD: SyntaxElementType = registerElementType(OVERRIDE_KEYWORD_ID, "override")
    val PRIVATE_KEYWORD: SyntaxElementType = registerElementType(PRIVATE_KEYWORD_ID, "private")
    val PUBLIC_KEYWORD: SyntaxElementType = registerElementType(PUBLIC_KEYWORD_ID, "public")
    val INTERNAL_KEYWORD: SyntaxElementType = registerElementType(INTERNAL_KEYWORD_ID, "internal")
    val PROTECTED_KEYWORD: SyntaxElementType = registerElementType(PROTECTED_KEYWORD_ID, "protected")
    val CATCH_KEYWORD: SyntaxElementType = registerElementType(CATCH_KEYWORD_ID, "catch")
    val OUT_KEYWORD: SyntaxElementType = registerElementType(OUT_KEYWORD_ID, "out")
    val VARARG_KEYWORD: SyntaxElementType = registerElementType(VARARG_KEYWORD_ID, "vararg")
    val REIFIED_KEYWORD: SyntaxElementType = registerElementType(REIFIED_KEYWORD_ID, "reified")
    val DYNAMIC_KEYWORD: SyntaxElementType = registerElementType(DYNAMIC_KEYWORD_ID, "dynamic")
    val COMPANION_KEYWORD: SyntaxElementType = registerElementType(COMPANION_KEYWORD_ID, "companion")
    val SEALED_KEYWORD: SyntaxElementType = registerElementType(SEALED_KEYWORD_ID, "sealed")

    val FINALLY_KEYWORD: SyntaxElementType = registerElementType(FINALLY_KEYWORD_ID, "finally")
    val FINAL_KEYWORD: SyntaxElementType = registerElementType(FINAL_KEYWORD_ID, "final")

    val LATEINIT_KEYWORD: SyntaxElementType = registerElementType(LATEINIT_KEYWORD_ID, "lateinit")

    val DATA_KEYWORD: SyntaxElementType = registerElementType(DATA_KEYWORD_ID, "data")
    val VALUE_KEYWORD: SyntaxElementType = registerElementType(VALUE_KEYWORD_ID, "value")
    val INLINE_KEYWORD: SyntaxElementType = registerElementType(INLINE_KEYWORD_ID, "inline")
    val NOINLINE_KEYWORD: SyntaxElementType = registerElementType(NOINLINE_KEYWORD_ID, "noinline")
    val TAILREC_KEYWORD: SyntaxElementType = registerElementType(TAILREC_KEYWORD_ID, "tailrec")
    val EXTERNAL_KEYWORD: SyntaxElementType = registerElementType(EXTERNAL_KEYWORD_ID, "external")
    val ANNOTATION_KEYWORD: SyntaxElementType = registerElementType(ANNOTATION_KEYWORD_ID, "annotation")
    val CROSSINLINE_KEYWORD: SyntaxElementType = registerElementType(CROSSINLINE_KEYWORD_ID, "crossinline")
    val OPERATOR_KEYWORD: SyntaxElementType = registerElementType(OPERATOR_KEYWORD_ID, "operator")
    val INFIX_KEYWORD: SyntaxElementType = registerElementType(INFIX_KEYWORD_ID, "infix")

    val CONST_KEYWORD: SyntaxElementType = registerElementType(CONST_KEYWORD_ID, "const")

    val SUSPEND_KEYWORD: SyntaxElementType = registerElementType(SUSPEND_KEYWORD_ID, "suspend")

    val EXPECT_KEYWORD: SyntaxElementType = registerElementType(EXPECT_KEYWORD_ID, "expect")
    val ACTUAL_KEYWORD: SyntaxElementType = registerElementType(ACTUAL_KEYWORD_ID, "actual")

    val KEYWORDS: SyntaxElementTypeSet = syntaxElementTypeSetOf(
        PACKAGE_KEYWORD, AS_KEYWORD, TYPE_ALIAS_KEYWORD, CLASS_KEYWORD, INTERFACE_KEYWORD,
        THIS_KEYWORD, SUPER_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD, FOR_KEYWORD,
        NULL_KEYWORD,
        TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD,
        IN_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD, CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD,
        ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD, TRY_KEYWORD, WHEN_KEYWORD,
        NOT_IN, NOT_IS, `AS_SAFE`,
        TYPEOF_KEYWORD
    )

    private val KEYWORDS_MAP: Map<String, SyntaxElementType> = KEYWORDS.associateBy { it.toString() }

    fun getKeyword(elementText: String?): SyntaxElementType? {
        return elementText?.let { KEYWORDS_MAP[it] }
    }

    val SOFT_KEYWORDS: SyntaxElementTypeSet = syntaxElementTypeSetOf(
        FILE_KEYWORD, IMPORT_KEYWORD, WHERE_KEYWORD, BY_KEYWORD, GET_KEYWORD,
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
    )

    private val SOFT_KEYWORDS_MAP: Map<String, SyntaxElementType> = SOFT_KEYWORDS.associateBy { it.toString() }

    fun isSoftKeyword(elementText: String?): Boolean = getSoftKeyword(elementText) != null

    fun getSoftKeyword(elementText: String?): SyntaxElementType? {
        return elementText?.let { SOFT_KEYWORDS_MAP[it] }
    }

    /*
        This is used in stub serialization:
        1. Do not change order.
        2. If you add an entry or change order, increase stub version.
     */
    val MODIFIER_KEYWORDS: SyntaxElementTypeSet =
        syntaxElementTypeSetOf(
            ABSTRACT_KEYWORD, ENUM_KEYWORD, CONTRACT_KEYWORD, OPEN_KEYWORD, INNER_KEYWORD, OVERRIDE_KEYWORD, PRIVATE_KEYWORD,
            PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD, OUT_KEYWORD, IN_KEYWORD, FINAL_KEYWORD, VARARG_KEYWORD,
            REIFIED_KEYWORD, COMPANION_KEYWORD, SEALED_KEYWORD, LATEINIT_KEYWORD,
            DATA_KEYWORD, INLINE_KEYWORD, NOINLINE_KEYWORD, TAILREC_KEYWORD, EXTERNAL_KEYWORD, ANNOTATION_KEYWORD, CROSSINLINE_KEYWORD,
            CONST_KEYWORD, OPERATOR_KEYWORD, INFIX_KEYWORD, SUSPEND_KEYWORD,
            EXPECT_KEYWORD, ACTUAL_KEYWORD, FUN_KEYWORD, VALUE_KEYWORD
        )

    val TYPE_MODIFIER_KEYWORDS: SyntaxElementTypeSet = syntaxElementTypeSetOf(SUSPEND_KEYWORD)
    val TYPE_ARGUMENT_MODIFIER_KEYWORDS: SyntaxElementTypeSet = syntaxElementTypeSetOf(IN_KEYWORD, OUT_KEYWORD)
    val RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS: SyntaxElementTypeSet = syntaxElementTypeSetOf(OUT_KEYWORD, VARARG_KEYWORD)

    val VISIBILITY_MODIFIERS: SyntaxElementTypeSet = syntaxElementTypeSetOf(PRIVATE_KEYWORD, PUBLIC_KEYWORD, INTERNAL_KEYWORD, PROTECTED_KEYWORD)
    val MODALITY_MODIFIERS: SyntaxElementTypeSet = syntaxElementTypeSetOf(ABSTRACT_KEYWORD, FINAL_KEYWORD, SEALED_KEYWORD, OPEN_KEYWORD)

    val WHITESPACES: SyntaxElementTypeSet = syntaxElementTypeSetOf(SyntaxTokenTypes.WHITE_SPACE)

    /**
     * Don't add KDocTokens to COMMENTS SyntaxElementTypeSet, because it is used in KotlinParserDefinition.getCommentTokens(),
     * and therefore all COMMENTS tokens will be ignored by SyntaxBuilder.
     */
    val COMMENTS: SyntaxElementTypeSet = syntaxElementTypeSetOf(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT)
    val WHITE_SPACE_OR_COMMENT_BIT_SET: SyntaxElementTypeSet = COMMENTS + WHITESPACES

    val STRINGS: SyntaxElementTypeSet = syntaxElementTypeSetOf(CHARACTER_LITERAL, REGULAR_STRING_PART)
    val OPERATIONS: SyntaxElementTypeSet = syntaxElementTypeSetOf(
        AS_KEYWORD, `AS_SAFE`, IS_KEYWORD, IN_KEYWORD, DOT, PLUSPLUS, MINUSMINUS, EXCLEXCL, MUL, PLUS,
        MINUS, EXCL, DIV, PERC, LT, GT, LTEQ, GTEQ, EQEQEQ, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR,
        SAFE_ACCESS, ELVIS,
        RANGE, RANGE_UNTIL, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ,
        NOT_IN, NOT_IS,
        IDENTIFIER
    )

    val AUGMENTED_ASSIGNMENTS: SyntaxElementTypeSet = syntaxElementTypeSetOf(PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ)
    val ALL_ASSIGNMENTS: SyntaxElementTypeSet = syntaxElementTypeSetOf(EQ, PLUSEQ, MINUSEQ, MULTEQ, PERCEQ, DIVEQ)
    val INCREMENT_AND_DECREMENT: SyntaxElementTypeSet = syntaxElementTypeSetOf(PLUSPLUS, MINUSMINUS)
}