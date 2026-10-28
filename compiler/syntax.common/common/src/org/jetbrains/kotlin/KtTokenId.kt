/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

object KtTokenId {
    const val DOC_COMMENT: Int = NO_ID + 1
    const val WHITE_SPACE: Int = DOC_COMMENT + 1

    const val EOF: Int = WHITE_SPACE + 1
    const val RESERVED: Int = EOF + 1
    const val BLOCK_COMMENT: Int = RESERVED + 1
    const val EOL_COMMENT: Int = BLOCK_COMMENT + 1
    const val SHEBANG_COMMENT: Int = EOL_COMMENT + 1

    const val INTEGER_LITERAL: Int = SHEBANG_COMMENT + 1
    const val FLOAT_LITERAL: Int = INTEGER_LITERAL + 1
    const val CHARACTER_LITERAL: Int = FLOAT_LITERAL + 1

    const val CLOSING_QUOTE: Int = CHARACTER_LITERAL + 1
    const val OPEN_QUOTE: Int = CLOSING_QUOTE + 1
    const val REGULAR_STRING_PART: Int = OPEN_QUOTE + 1
    const val ESCAPE_SEQUENCE: Int = REGULAR_STRING_PART + 1
    const val SHORT_TEMPLATE_ENTRY_START: Int = ESCAPE_SEQUENCE + 1
    const val LONG_TEMPLATE_ENTRY_START: Int = SHORT_TEMPLATE_ENTRY_START + 1
    const val LONG_TEMPLATE_ENTRY_END: Int = LONG_TEMPLATE_ENTRY_START + 1
    const val DANGLING_NEWLINE: Int = LONG_TEMPLATE_ENTRY_END + 1

    const val PACKAGE_KEYWORD: Int = DANGLING_NEWLINE + 1
    const val AS_KEYWORD: Int = PACKAGE_KEYWORD + 1
    const val TYPE_ALIAS_KEYWORD: Int = AS_KEYWORD + 1
    const val CLASS_KEYWORD: Int = TYPE_ALIAS_KEYWORD + 1
    const val THIS_KEYWORD: Int = CLASS_KEYWORD + 1
    const val SUPER_KEYWORD: Int = THIS_KEYWORD + 1
    const val VAL_KEYWORD: Int = SUPER_KEYWORD + 1
    const val VAR_KEYWORD: Int = VAL_KEYWORD + 1
    const val FUN_MODIFIER: Int = VAR_KEYWORD + 1
    const val FOR_KEYWORD: Int = FUN_MODIFIER + 1
    const val NULL_KEYWORD: Int = FOR_KEYWORD + 1
    const val TRUE_KEYWORD: Int = NULL_KEYWORD + 1
    const val FALSE_KEYWORD: Int = TRUE_KEYWORD + 1
    const val IS_KEYWORD: Int = FALSE_KEYWORD + 1
    const val IN_MODIFIER: Int = IS_KEYWORD + 1
    const val THROW_KEYWORD: Int = IN_MODIFIER + 1
    const val RETURN_KEYWORD: Int = THROW_KEYWORD + 1
    const val BREAK_KEYWORD: Int = RETURN_KEYWORD + 1
    const val CONTINUE_KEYWORD: Int = BREAK_KEYWORD + 1
    const val OBJECT_KEYWORD: Int = CONTINUE_KEYWORD + 1
    const val IF_KEYWORD: Int = OBJECT_KEYWORD + 1
    const val TRY_KEYWORD: Int = IF_KEYWORD + 1
    const val ELSE_KEYWORD: Int = TRY_KEYWORD + 1
    const val WHILE_KEYWORD: Int = ELSE_KEYWORD + 1
    const val DO_KEYWORD: Int = WHILE_KEYWORD + 1
    const val WHEN_KEYWORD: Int = DO_KEYWORD + 1
    const val INTERFACE_KEYWORD: Int = WHEN_KEYWORD + 1

    const val TYPEOF_KEYWORD: Int = INTERFACE_KEYWORD + 1

    const val AS_SAFE: Int = TYPEOF_KEYWORD + 1

    const val IDENTIFIER: Int = AS_SAFE + 1
    const val FIELD_IDENTIFIER: Int = IDENTIFIER + 1
    const val LBRACKET: Int = FIELD_IDENTIFIER + 1
    const val RBRACKET: Int = LBRACKET + 1
    const val LBRACE: Int = RBRACKET + 1
    const val RBRACE: Int = LBRACE + 1
    const val LPAR: Int = RBRACE + 1
    const val RPAR: Int = LPAR + 1
    const val DOT: Int = RPAR + 1
    const val PLUSPLUS: Int = DOT + 1
    const val MINUSMINUS: Int = PLUSPLUS + 1
    const val MUL: Int = MINUSMINUS + 1
    const val PLUS: Int = MUL + 1
    const val MINUS: Int = PLUS + 1
    const val EXCL: Int = MINUS + 1
    const val DIV: Int = EXCL + 1
    const val PERC: Int = DIV + 1
    const val LT: Int = PERC + 1
    const val GT: Int = LT + 1
    const val LTEQ: Int = GT + 1
    const val GTEQ: Int = LTEQ + 1
    const val EQEQEQ: Int = GTEQ + 1
    const val ARROW: Int = EQEQEQ + 1
    const val DOUBLE_ARROW: Int = ARROW + 1
    const val EXCLEQEQEQ: Int = DOUBLE_ARROW + 1
    const val EQEQ: Int = EXCLEQEQEQ + 1
    const val EXCLEQ: Int = EQEQ + 1
    const val EXCLEXCL: Int = EXCLEQ + 1
    const val ANDAND: Int = EXCLEXCL + 1
    const val AND: Int = ANDAND + 1
    const val OROR: Int = AND + 1
    const val SAFE_ACCESS: Int = OROR + 1
    const val ELVIS: Int = SAFE_ACCESS + 1
    const val QUEST: Int = ELVIS + 1
    const val COLONCOLON: Int = QUEST + 1
    const val COLON: Int = COLONCOLON + 1
    const val SEMICOLON: Int = COLON + 1
    const val DOUBLE_SEMICOLON: Int = SEMICOLON + 1
    const val RANGE: Int = DOUBLE_SEMICOLON + 1
    const val RANGE_UNTIL: Int = RANGE + 1
    const val EQ: Int = RANGE_UNTIL + 1
    const val MULTEQ: Int = EQ + 1
    const val DIVEQ: Int = MULTEQ + 1
    const val PERCEQ: Int = DIVEQ + 1
    const val PLUSEQ: Int = PERCEQ + 1
    const val MINUSEQ: Int = PLUSEQ + 1
    const val NOT_IN: Int = MINUSEQ + 1
    const val NOT_IS: Int = NOT_IN + 1
    const val HASH: Int = NOT_IS + 1
    const val AT: Int = HASH + 1
    const val COMMA: Int = AT + 1
    const val EOL_OR_SEMICOLON: Int = COMMA + 1
    const val FILE_KEYWORD: Int = EOL_OR_SEMICOLON + 1
    const val FIELD_KEYWORD: Int = FILE_KEYWORD + 1
    const val PROPERTY_KEYWORD: Int = FIELD_KEYWORD + 1
    const val RECEIVER_KEYWORD: Int = PROPERTY_KEYWORD + 1
    const val PARAM_KEYWORD: Int = RECEIVER_KEYWORD + 1
    const val SETPARAM_KEYWORD: Int = PARAM_KEYWORD + 1
    const val DELEGATE_KEYWORD: Int = SETPARAM_KEYWORD + 1
    const val IMPORT_KEYWORD: Int = DELEGATE_KEYWORD + 1
    const val WHERE_KEYWORD: Int = IMPORT_KEYWORD + 1
    const val BY_KEYWORD: Int = WHERE_KEYWORD + 1
    const val GET_KEYWORD: Int = BY_KEYWORD + 1
    const val SET_KEYWORD: Int = GET_KEYWORD + 1
    const val CONSTRUCTOR_KEYWORD: Int = SET_KEYWORD + 1
    const val INIT_KEYWORD: Int = CONSTRUCTOR_KEYWORD + 1
    const val CONTEXT_KEYWORD: Int = INIT_KEYWORD + 1
    const val CATCH_KEYWORD: Int = CONTEXT_KEYWORD + 1
    const val DYNAMIC_KEYWORD: Int = CATCH_KEYWORD + 1
    const val FINALLY_KEYWORD: Int = DYNAMIC_KEYWORD + 1

    const val ABSTRACT_MODIFIER: Int = FINALLY_KEYWORD + 1
    const val ENUM_MODIFIER: Int = ABSTRACT_MODIFIER + 1
    const val CONTRACT_MODIFIER: Int = ENUM_MODIFIER + 1
    const val OPEN_MODIFIER: Int = CONTRACT_MODIFIER + 1
    const val INNER_MODIFIER: Int = OPEN_MODIFIER + 1
    const val OVERRIDE_MODIFIER: Int = INNER_MODIFIER + 1
    const val PRIVATE_MODIFIER: Int = OVERRIDE_MODIFIER + 1
    const val PUBLIC_MODIFIER: Int = PRIVATE_MODIFIER + 1
    const val INTERNAL_MODIFIER: Int = PUBLIC_MODIFIER + 1
    const val PROTECTED_MODIFIER: Int = INTERNAL_MODIFIER + 1
    const val OUT_MODIFIER: Int = PROTECTED_MODIFIER + 1
    const val VARARG_MODIFIER: Int = OUT_MODIFIER + 1
    const val REIFIED_MODIFIER: Int = VARARG_MODIFIER + 1
    const val COMPANION_MODIFIER: Int = REIFIED_MODIFIER + 1
    const val SEALED_MODIFIER: Int = COMPANION_MODIFIER + 1
    const val FINAL_MODIFIER: Int = SEALED_MODIFIER + 1
    const val LATEINIT_MODIFIER: Int = FINAL_MODIFIER + 1
    const val DATA_MODIFIER: Int = LATEINIT_MODIFIER + 1
    const val VALUE_MODIFIER: Int = DATA_MODIFIER + 1
    const val INLINE_MODIFIER: Int = VALUE_MODIFIER + 1
    const val NOINLINE_MODIFIER: Int = INLINE_MODIFIER + 1
    const val TAILREC_MODIFIER: Int = NOINLINE_MODIFIER + 1
    const val EXTERNAL_MODIFIER: Int = TAILREC_MODIFIER + 1
    const val ANNOTATION_MODIFIER: Int = EXTERNAL_MODIFIER + 1
    const val CROSSINLINE_MODIFIER: Int = ANNOTATION_MODIFIER + 1
    const val OPERATOR_MODIFIER: Int = CROSSINLINE_MODIFIER + 1
    const val INFIX_MODIFIER: Int = OPERATOR_MODIFIER + 1
    const val CONST_MODIFIER: Int = INFIX_MODIFIER + 1
    const val SUSPEND_MODIFIER: Int = CONST_MODIFIER + 1
    const val EXPECT_MODIFIER: Int = SUSPEND_MODIFIER + 1
    const val ACTUAL_MODIFIER: Int = EXPECT_MODIFIER + 1
    const val INTERPOLATION_PREFIX: Int = ACTUAL_MODIFIER + 1
    const val ALL_KEYWORD: Int = INTERPOLATION_PREFIX + 1
    const val ERROR_MODIFIER: Int = ALL_KEYWORD + 1
    const val OR: Int = ERROR_MODIFIER + 1
    const val ERROR_SAFE_ACCESS: Int = OR + 1
    /**
     * Remember to update first const in [KtNodeTypeId] after adding new tokens here ^^^
     */
}
