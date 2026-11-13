/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

public object KotlinSyntaxTokenIds {
    public const val DOC_COMMENT: Int = NO_ID + 1
    public const val WHITE_SPACE: Int = DOC_COMMENT + 1

    public const val EOF: Int = WHITE_SPACE + 1
    public const val RESERVED: Int = EOF + 1
    public const val BLOCK_COMMENT: Int = RESERVED + 1
    public const val EOL_COMMENT: Int = BLOCK_COMMENT + 1
    public const val SHEBANG_COMMENT: Int = EOL_COMMENT + 1

    public const val INTEGER_LITERAL: Int = SHEBANG_COMMENT + 1
    public const val FLOAT_LITERAL: Int = INTEGER_LITERAL + 1
    public const val CHARACTER_LITERAL: Int = FLOAT_LITERAL + 1

    public const val CLOSING_QUOTE: Int = CHARACTER_LITERAL + 1
    public const val OPEN_QUOTE: Int = CLOSING_QUOTE + 1
    public const val REGULAR_STRING_PART: Int = OPEN_QUOTE + 1
    public const val ESCAPE_SEQUENCE: Int = REGULAR_STRING_PART + 1
    public const val SHORT_TEMPLATE_ENTRY_START: Int = ESCAPE_SEQUENCE + 1
    public const val LONG_TEMPLATE_ENTRY_START: Int = SHORT_TEMPLATE_ENTRY_START + 1
    public const val LONG_TEMPLATE_ENTRY_END: Int = LONG_TEMPLATE_ENTRY_START + 1
    public const val DANGLING_NEWLINE: Int = LONG_TEMPLATE_ENTRY_END + 1

    public const val PACKAGE_KEYWORD: Int = DANGLING_NEWLINE + 1
    public const val AS_KEYWORD: Int = PACKAGE_KEYWORD + 1
    public const val TYPE_ALIAS_KEYWORD: Int = AS_KEYWORD + 1
    public const val CLASS_KEYWORD: Int = TYPE_ALIAS_KEYWORD + 1
    public const val THIS_KEYWORD: Int = CLASS_KEYWORD + 1
    public const val SUPER_KEYWORD: Int = THIS_KEYWORD + 1
    public const val VAL_KEYWORD: Int = SUPER_KEYWORD + 1
    public const val VAR_KEYWORD: Int = VAL_KEYWORD + 1
    public const val FUN_MODIFIER: Int = VAR_KEYWORD + 1
    public const val FOR_KEYWORD: Int = FUN_MODIFIER + 1
    public const val NULL_KEYWORD: Int = FOR_KEYWORD + 1
    public const val TRUE_KEYWORD: Int = NULL_KEYWORD + 1
    public const val FALSE_KEYWORD: Int = TRUE_KEYWORD + 1
    public const val IS_KEYWORD: Int = FALSE_KEYWORD + 1
    public const val IN_MODIFIER: Int = IS_KEYWORD + 1
    public const val THROW_KEYWORD: Int = IN_MODIFIER + 1
    public const val RETURN_KEYWORD: Int = THROW_KEYWORD + 1
    public const val BREAK_KEYWORD: Int = RETURN_KEYWORD + 1
    public const val CONTINUE_KEYWORD: Int = BREAK_KEYWORD + 1
    public const val OBJECT_KEYWORD: Int = CONTINUE_KEYWORD + 1
    public const val IF_KEYWORD: Int = OBJECT_KEYWORD + 1
    public const val TRY_KEYWORD: Int = IF_KEYWORD + 1
    public const val ELSE_KEYWORD: Int = TRY_KEYWORD + 1
    public const val WHILE_KEYWORD: Int = ELSE_KEYWORD + 1
    public const val DO_KEYWORD: Int = WHILE_KEYWORD + 1
    public const val WHEN_KEYWORD: Int = DO_KEYWORD + 1
    public const val INTERFACE_KEYWORD: Int = WHEN_KEYWORD + 1

    public const val TYPEOF_KEYWORD: Int = INTERFACE_KEYWORD + 1

    public const val AS_SAFE: Int = TYPEOF_KEYWORD + 1

    public const val IDENTIFIER: Int = AS_SAFE + 1
    public const val FIELD_IDENTIFIER: Int = IDENTIFIER + 1
    public const val LBRACKET: Int = FIELD_IDENTIFIER + 1
    public const val RBRACKET: Int = LBRACKET + 1
    public const val LBRACE: Int = RBRACKET + 1
    public const val RBRACE: Int = LBRACE + 1
    public const val LPAR: Int = RBRACE + 1
    public const val RPAR: Int = LPAR + 1
    public const val DOT: Int = RPAR + 1
    public const val PLUSPLUS: Int = DOT + 1
    public const val MINUSMINUS: Int = PLUSPLUS + 1
    public const val MUL: Int = MINUSMINUS + 1
    public const val PLUS: Int = MUL + 1
    public const val MINUS: Int = PLUS + 1
    public const val EXCL: Int = MINUS + 1
    public const val DIV: Int = EXCL + 1
    public const val PERC: Int = DIV + 1
    public const val LT: Int = PERC + 1
    public const val GT: Int = LT + 1
    public const val LTEQ: Int = GT + 1
    public const val GTEQ: Int = LTEQ + 1
    public const val EQEQEQ: Int = GTEQ + 1
    public const val ARROW: Int = EQEQEQ + 1
    public const val DOUBLE_ARROW: Int = ARROW + 1
    public const val EXCLEQEQEQ: Int = DOUBLE_ARROW + 1
    public const val EQEQ: Int = EXCLEQEQEQ + 1
    public const val EXCLEQ: Int = EQEQ + 1
    public const val EXCLEXCL: Int = EXCLEQ + 1
    public const val ANDAND: Int = EXCLEXCL + 1
    public const val AND: Int = ANDAND + 1
    public const val OROR: Int = AND + 1
    public const val SAFE_ACCESS: Int = OROR + 1
    public const val ELVIS: Int = SAFE_ACCESS + 1
    public const val QUEST: Int = ELVIS + 1
    public const val COLONCOLON: Int = QUEST + 1
    public const val COLON: Int = COLONCOLON + 1
    public const val SEMICOLON: Int = COLON + 1
    public const val DOUBLE_SEMICOLON: Int = SEMICOLON + 1
    public const val RANGE: Int = DOUBLE_SEMICOLON + 1
    public const val RANGE_UNTIL: Int = RANGE + 1
    public const val EQ: Int = RANGE_UNTIL + 1
    public const val MULTEQ: Int = EQ + 1
    public const val DIVEQ: Int = MULTEQ + 1
    public const val PERCEQ: Int = DIVEQ + 1
    public const val PLUSEQ: Int = PERCEQ + 1
    public const val MINUSEQ: Int = PLUSEQ + 1
    public const val NOT_IN: Int = MINUSEQ + 1
    public const val NOT_IS: Int = NOT_IN + 1
    public const val HASH: Int = NOT_IS + 1
    public const val AT: Int = HASH + 1
    public const val COMMA: Int = AT + 1
    public const val EOL_OR_SEMICOLON: Int = COMMA + 1
    public const val FILE_KEYWORD: Int = EOL_OR_SEMICOLON + 1
    public const val FIELD_KEYWORD: Int = FILE_KEYWORD + 1
    public const val PROPERTY_KEYWORD: Int = FIELD_KEYWORD + 1
    public const val RECEIVER_KEYWORD: Int = PROPERTY_KEYWORD + 1
    public const val PARAM_KEYWORD: Int = RECEIVER_KEYWORD + 1
    public const val SETPARAM_KEYWORD: Int = PARAM_KEYWORD + 1
    public const val DELEGATE_KEYWORD: Int = SETPARAM_KEYWORD + 1
    public const val IMPORT_KEYWORD: Int = DELEGATE_KEYWORD + 1
    public const val WHERE_KEYWORD: Int = IMPORT_KEYWORD + 1
    public const val BY_KEYWORD: Int = WHERE_KEYWORD + 1
    public const val GET_KEYWORD: Int = BY_KEYWORD + 1
    public const val SET_KEYWORD: Int = GET_KEYWORD + 1
    public const val CONSTRUCTOR_KEYWORD: Int = SET_KEYWORD + 1
    public const val INIT_KEYWORD: Int = CONSTRUCTOR_KEYWORD + 1
    public const val CONTEXT_KEYWORD: Int = INIT_KEYWORD + 1
    public const val CATCH_KEYWORD: Int = CONTEXT_KEYWORD + 1
    public const val DYNAMIC_KEYWORD: Int = CATCH_KEYWORD + 1
    public const val FINALLY_KEYWORD: Int = DYNAMIC_KEYWORD + 1

    public const val ABSTRACT_MODIFIER: Int = FINALLY_KEYWORD + 1
    public const val ENUM_MODIFIER: Int = ABSTRACT_MODIFIER + 1
    public const val CONTRACT_MODIFIER: Int = ENUM_MODIFIER + 1
    public const val OPEN_MODIFIER: Int = CONTRACT_MODIFIER + 1
    public const val INNER_MODIFIER: Int = OPEN_MODIFIER + 1
    public const val OVERRIDE_MODIFIER: Int = INNER_MODIFIER + 1
    public const val PRIVATE_MODIFIER: Int = OVERRIDE_MODIFIER + 1
    public const val PUBLIC_MODIFIER: Int = PRIVATE_MODIFIER + 1
    public const val INTERNAL_MODIFIER: Int = PUBLIC_MODIFIER + 1
    public const val PROTECTED_MODIFIER: Int = INTERNAL_MODIFIER + 1
    public const val OUT_MODIFIER: Int = PROTECTED_MODIFIER + 1
    public const val VARARG_MODIFIER: Int = OUT_MODIFIER + 1
    public const val REIFIED_MODIFIER: Int = VARARG_MODIFIER + 1
    public const val COMPANION_MODIFIER: Int = REIFIED_MODIFIER + 1
    public const val SEALED_MODIFIER: Int = COMPANION_MODIFIER + 1
    public const val FINAL_MODIFIER: Int = SEALED_MODIFIER + 1
    public const val LATEINIT_MODIFIER: Int = FINAL_MODIFIER + 1
    public const val DATA_MODIFIER: Int = LATEINIT_MODIFIER + 1
    public const val VALUE_MODIFIER: Int = DATA_MODIFIER + 1
    public const val INLINE_MODIFIER: Int = VALUE_MODIFIER + 1
    public const val NOINLINE_MODIFIER: Int = INLINE_MODIFIER + 1
    public const val TAILREC_MODIFIER: Int = NOINLINE_MODIFIER + 1
    public const val EXTERNAL_MODIFIER: Int = TAILREC_MODIFIER + 1
    public const val ANNOTATION_MODIFIER: Int = EXTERNAL_MODIFIER + 1
    public const val CROSSINLINE_MODIFIER: Int = ANNOTATION_MODIFIER + 1
    public const val OPERATOR_MODIFIER: Int = CROSSINLINE_MODIFIER + 1
    public const val INFIX_MODIFIER: Int = OPERATOR_MODIFIER + 1
    public const val CONST_MODIFIER: Int = INFIX_MODIFIER + 1
    public const val SUSPEND_MODIFIER: Int = CONST_MODIFIER + 1
    public const val EXPECT_MODIFIER: Int = SUSPEND_MODIFIER + 1
    public const val ACTUAL_MODIFIER: Int = EXPECT_MODIFIER + 1
    public const val INTERPOLATION_PREFIX: Int = ACTUAL_MODIFIER + 1
    public const val ALL_KEYWORD: Int = INTERPOLATION_PREFIX + 1
    public const val ERROR_MODIFIER: Int = ALL_KEYWORD + 1
    public const val OR: Int = ERROR_MODIFIER + 1
    public const val ERROR_SAFE_ACCESS: Int = OR + 1
    /**
     * Remember to update first const in [KotlinSyntaxNodeTypeIds] after adding new tokens here ^^^
     */
}
