/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

public object KotlinSyntaxTokenIds {
    public const val DOC_COMMENT: SyntaxId = BasicSyntaxElementTypeIds.ERROR_ELEMENT + 1
    public const val EOF: SyntaxId = DOC_COMMENT + 1
    public const val RESERVED: SyntaxId = EOF + 1
    public const val BLOCK_COMMENT: SyntaxId = RESERVED + 1
    public const val EOL_COMMENT: SyntaxId = BLOCK_COMMENT + 1
    public const val SHEBANG_COMMENT: SyntaxId = EOL_COMMENT + 1

    public const val INTEGER_LITERAL: SyntaxId = SHEBANG_COMMENT + 1
    public const val FLOAT_LITERAL: SyntaxId = INTEGER_LITERAL + 1
    public const val CHARACTER_LITERAL: SyntaxId = FLOAT_LITERAL + 1

    public const val CLOSING_QUOTE: SyntaxId = CHARACTER_LITERAL + 1
    public const val OPEN_QUOTE: SyntaxId = CLOSING_QUOTE + 1
    public const val REGULAR_STRING_PART: SyntaxId = OPEN_QUOTE + 1
    public const val ESCAPE_SEQUENCE: SyntaxId = REGULAR_STRING_PART + 1
    public const val SHORT_TEMPLATE_ENTRY_START: SyntaxId = ESCAPE_SEQUENCE + 1
    public const val LONG_TEMPLATE_ENTRY_START: SyntaxId = SHORT_TEMPLATE_ENTRY_START + 1
    public const val LONG_TEMPLATE_ENTRY_END: SyntaxId = LONG_TEMPLATE_ENTRY_START + 1
    public const val DANGLING_NEWLINE: SyntaxId = LONG_TEMPLATE_ENTRY_END + 1

    public const val PACKAGE_KEYWORD: SyntaxId = DANGLING_NEWLINE + 1
    public const val AS_KEYWORD: SyntaxId = PACKAGE_KEYWORD + 1
    public const val TYPE_ALIAS_KEYWORD: SyntaxId = AS_KEYWORD + 1
    public const val CLASS_KEYWORD: SyntaxId = TYPE_ALIAS_KEYWORD + 1
    public const val THIS_KEYWORD: SyntaxId = CLASS_KEYWORD + 1
    public const val SUPER_KEYWORD: SyntaxId = THIS_KEYWORD + 1
    public const val VAL_KEYWORD: SyntaxId = SUPER_KEYWORD + 1
    public const val VAR_KEYWORD: SyntaxId = VAL_KEYWORD + 1
    public const val FUN_MODIFIER: SyntaxId = VAR_KEYWORD + 1
    public const val FOR_KEYWORD: SyntaxId = FUN_MODIFIER + 1
    public const val NULL_KEYWORD: SyntaxId = FOR_KEYWORD + 1
    public const val TRUE_KEYWORD: SyntaxId = NULL_KEYWORD + 1
    public const val FALSE_KEYWORD: SyntaxId = TRUE_KEYWORD + 1
    public const val IS_KEYWORD: SyntaxId = FALSE_KEYWORD + 1
    public const val IN_MODIFIER: SyntaxId = IS_KEYWORD + 1
    public const val THROW_KEYWORD: SyntaxId = IN_MODIFIER + 1
    public const val RETURN_KEYWORD: SyntaxId = THROW_KEYWORD + 1
    public const val BREAK_KEYWORD: SyntaxId = RETURN_KEYWORD + 1
    public const val CONTINUE_KEYWORD: SyntaxId = BREAK_KEYWORD + 1
    public const val OBJECT_KEYWORD: SyntaxId = CONTINUE_KEYWORD + 1
    public const val IF_KEYWORD: SyntaxId = OBJECT_KEYWORD + 1
    public const val TRY_KEYWORD: SyntaxId = IF_KEYWORD + 1
    public const val ELSE_KEYWORD: SyntaxId = TRY_KEYWORD + 1
    public const val WHILE_KEYWORD: SyntaxId = ELSE_KEYWORD + 1
    public const val DO_KEYWORD: SyntaxId = WHILE_KEYWORD + 1
    public const val WHEN_KEYWORD: SyntaxId = DO_KEYWORD + 1
    public const val INTERFACE_KEYWORD: SyntaxId = WHEN_KEYWORD + 1

    public const val TYPEOF_KEYWORD: SyntaxId = INTERFACE_KEYWORD + 1

    public const val AS_SAFE: SyntaxId = TYPEOF_KEYWORD + 1

    public const val IDENTIFIER: SyntaxId = AS_SAFE + 1
    public const val FIELD_IDENTIFIER: SyntaxId = IDENTIFIER + 1
    public const val LBRACKET: SyntaxId = FIELD_IDENTIFIER + 1
    public const val RBRACKET: SyntaxId = LBRACKET + 1
    public const val LBRACE: SyntaxId = RBRACKET + 1
    public const val RBRACE: SyntaxId = LBRACE + 1
    public const val LPAR: SyntaxId = RBRACE + 1
    public const val RPAR: SyntaxId = LPAR + 1
    public const val DOT: SyntaxId = RPAR + 1
    public const val PLUSPLUS: SyntaxId = DOT + 1
    public const val MINUSMINUS: SyntaxId = PLUSPLUS + 1
    public const val MUL: SyntaxId = MINUSMINUS + 1
    public const val PLUS: SyntaxId = MUL + 1
    public const val MINUS: SyntaxId = PLUS + 1
    public const val EXCL: SyntaxId = MINUS + 1
    public const val DIV: SyntaxId = EXCL + 1
    public const val PERC: SyntaxId = DIV + 1
    public const val LT: SyntaxId = PERC + 1
    public const val GT: SyntaxId = LT + 1
    public const val LTEQ: SyntaxId = GT + 1
    public const val GTEQ: SyntaxId = LTEQ + 1
    public const val EQEQEQ: SyntaxId = GTEQ + 1
    public const val ARROW: SyntaxId = EQEQEQ + 1
    public const val DOUBLE_ARROW: SyntaxId = ARROW + 1
    public const val EXCLEQEQEQ: SyntaxId = DOUBLE_ARROW + 1
    public const val EQEQ: SyntaxId = EXCLEQEQEQ + 1
    public const val EXCLEQ: SyntaxId = EQEQ + 1
    public const val EXCLEXCL: SyntaxId = EXCLEQ + 1
    public const val ANDAND: SyntaxId = EXCLEXCL + 1
    public const val AND: SyntaxId = ANDAND + 1
    public const val OROR: SyntaxId = AND + 1
    public const val SAFE_ACCESS: SyntaxId = OROR + 1
    public const val ELVIS: SyntaxId = SAFE_ACCESS + 1
    public const val QUEST: SyntaxId = ELVIS + 1
    public const val COLONCOLON: SyntaxId = QUEST + 1
    public const val COLON: SyntaxId = COLONCOLON + 1
    public const val SEMICOLON: SyntaxId = COLON + 1
    public const val DOUBLE_SEMICOLON: SyntaxId = SEMICOLON + 1
    public const val RANGE: SyntaxId = DOUBLE_SEMICOLON + 1
    public const val RANGE_UNTIL: SyntaxId = RANGE + 1
    public const val EQ: SyntaxId = RANGE_UNTIL + 1
    public const val MULTEQ: SyntaxId = EQ + 1
    public const val DIVEQ: SyntaxId = MULTEQ + 1
    public const val PERCEQ: SyntaxId = DIVEQ + 1
    public const val PLUSEQ: SyntaxId = PERCEQ + 1
    public const val MINUSEQ: SyntaxId = PLUSEQ + 1
    public const val NOT_IN: SyntaxId = MINUSEQ + 1
    public const val NOT_IS: SyntaxId = NOT_IN + 1
    public const val HASH: SyntaxId = NOT_IS + 1
    public const val AT: SyntaxId = HASH + 1
    public const val COMMA: SyntaxId = AT + 1
    public const val EOL_OR_SEMICOLON: SyntaxId = COMMA + 1
    public const val FILE_KEYWORD: SyntaxId = EOL_OR_SEMICOLON + 1
    public const val FIELD_KEYWORD: SyntaxId = FILE_KEYWORD + 1
    public const val PROPERTY_KEYWORD: SyntaxId = FIELD_KEYWORD + 1
    public const val RECEIVER_KEYWORD: SyntaxId = PROPERTY_KEYWORD + 1
    public const val PARAM_KEYWORD: SyntaxId = RECEIVER_KEYWORD + 1
    public const val SETPARAM_KEYWORD: SyntaxId = PARAM_KEYWORD + 1
    public const val DELEGATE_KEYWORD: SyntaxId = SETPARAM_KEYWORD + 1
    public const val IMPORT_KEYWORD: SyntaxId = DELEGATE_KEYWORD + 1
    public const val WHERE_KEYWORD: SyntaxId = IMPORT_KEYWORD + 1
    public const val BY_KEYWORD: SyntaxId = WHERE_KEYWORD + 1
    public const val GET_KEYWORD: SyntaxId = BY_KEYWORD + 1
    public const val SET_KEYWORD: SyntaxId = GET_KEYWORD + 1
    public const val CONSTRUCTOR_KEYWORD: SyntaxId = SET_KEYWORD + 1
    public const val INIT_KEYWORD: SyntaxId = CONSTRUCTOR_KEYWORD + 1
    public const val CONTEXT_KEYWORD: SyntaxId = INIT_KEYWORD + 1
    public const val CATCH_KEYWORD: SyntaxId = CONTEXT_KEYWORD + 1
    public const val DYNAMIC_KEYWORD: SyntaxId = CATCH_KEYWORD + 1
    public const val FINALLY_KEYWORD: SyntaxId = DYNAMIC_KEYWORD + 1

    public const val ABSTRACT_MODIFIER: SyntaxId = FINALLY_KEYWORD + 1
    public const val ENUM_MODIFIER: SyntaxId = ABSTRACT_MODIFIER + 1
    public const val CONTRACT_MODIFIER: SyntaxId = ENUM_MODIFIER + 1
    public const val OPEN_MODIFIER: SyntaxId = CONTRACT_MODIFIER + 1
    public const val INNER_MODIFIER: SyntaxId = OPEN_MODIFIER + 1
    public const val OVERRIDE_MODIFIER: SyntaxId = INNER_MODIFIER + 1
    public const val PRIVATE_MODIFIER: SyntaxId = OVERRIDE_MODIFIER + 1
    public const val PUBLIC_MODIFIER: SyntaxId = PRIVATE_MODIFIER + 1
    public const val INTERNAL_MODIFIER: SyntaxId = PUBLIC_MODIFIER + 1
    public const val PROTECTED_MODIFIER: SyntaxId = INTERNAL_MODIFIER + 1
    public const val OUT_MODIFIER: SyntaxId = PROTECTED_MODIFIER + 1
    public const val VARARG_MODIFIER: SyntaxId = OUT_MODIFIER + 1
    public const val REIFIED_MODIFIER: SyntaxId = VARARG_MODIFIER + 1
    public const val COMPANION_MODIFIER: SyntaxId = REIFIED_MODIFIER + 1
    public const val SEALED_MODIFIER: SyntaxId = COMPANION_MODIFIER + 1
    public const val FINAL_MODIFIER: SyntaxId = SEALED_MODIFIER + 1
    public const val LATEINIT_MODIFIER: SyntaxId = FINAL_MODIFIER + 1
    public const val DATA_MODIFIER: SyntaxId = LATEINIT_MODIFIER + 1
    public const val VALUE_MODIFIER: SyntaxId = DATA_MODIFIER + 1
    public const val INLINE_MODIFIER: SyntaxId = VALUE_MODIFIER + 1
    public const val NOINLINE_MODIFIER: SyntaxId = INLINE_MODIFIER + 1
    public const val TAILREC_MODIFIER: SyntaxId = NOINLINE_MODIFIER + 1
    public const val EXTERNAL_MODIFIER: SyntaxId = TAILREC_MODIFIER + 1
    public const val ANNOTATION_MODIFIER: SyntaxId = EXTERNAL_MODIFIER + 1
    public const val CROSSINLINE_MODIFIER: SyntaxId = ANNOTATION_MODIFIER + 1
    public const val OPERATOR_MODIFIER: SyntaxId = CROSSINLINE_MODIFIER + 1
    public const val INFIX_MODIFIER: SyntaxId = OPERATOR_MODIFIER + 1
    public const val CONST_MODIFIER: SyntaxId = INFIX_MODIFIER + 1
    public const val SUSPEND_MODIFIER: SyntaxId = CONST_MODIFIER + 1
    public const val EXPECT_MODIFIER: SyntaxId = SUSPEND_MODIFIER + 1
    public const val ACTUAL_MODIFIER: SyntaxId = EXPECT_MODIFIER + 1
    public const val INTERPOLATION_PREFIX: SyntaxId = ACTUAL_MODIFIER + 1
    public const val ALL_KEYWORD: SyntaxId = INTERPOLATION_PREFIX + 1
    public const val ERROR_MODIFIER: SyntaxId = ALL_KEYWORD + 1
    public const val OR: SyntaxId = ERROR_MODIFIER + 1
    public const val ERROR_SAFE_ACCESS: SyntaxId = OR + 1
    /**
     * Remember to update first const in [KotlinSyntaxNodeTypeIds] after adding new tokens here ^^^
     */
}
