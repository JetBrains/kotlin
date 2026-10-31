/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

object KtTokenId {
    const val DOC_COMMENT: SyntaxId = BasicElementTypeId.ERROR_ELEMENT + 1
    const val EOF: SyntaxId = DOC_COMMENT + 1
    const val RESERVED: SyntaxId = EOF + 1
    const val BLOCK_COMMENT: SyntaxId = RESERVED + 1
    const val EOL_COMMENT: SyntaxId = BLOCK_COMMENT + 1
    const val SHEBANG_COMMENT: SyntaxId = EOL_COMMENT + 1

    const val INTEGER_LITERAL: SyntaxId = SHEBANG_COMMENT + 1
    const val FLOAT_LITERAL: SyntaxId = INTEGER_LITERAL + 1
    const val CHARACTER_LITERAL: SyntaxId = FLOAT_LITERAL + 1

    const val CLOSING_QUOTE: SyntaxId = CHARACTER_LITERAL + 1
    const val OPEN_QUOTE: SyntaxId = CLOSING_QUOTE + 1
    const val REGULAR_STRING_PART: SyntaxId = OPEN_QUOTE + 1
    const val ESCAPE_SEQUENCE: SyntaxId = REGULAR_STRING_PART + 1
    const val SHORT_TEMPLATE_ENTRY_START: SyntaxId = ESCAPE_SEQUENCE + 1
    const val LONG_TEMPLATE_ENTRY_START: SyntaxId = SHORT_TEMPLATE_ENTRY_START + 1
    const val LONG_TEMPLATE_ENTRY_END: SyntaxId = LONG_TEMPLATE_ENTRY_START + 1
    const val DANGLING_NEWLINE: SyntaxId = LONG_TEMPLATE_ENTRY_END + 1

    const val PACKAGE_KEYWORD: SyntaxId = DANGLING_NEWLINE + 1
    const val AS_KEYWORD: SyntaxId = PACKAGE_KEYWORD + 1
    const val TYPE_ALIAS_KEYWORD: SyntaxId = AS_KEYWORD + 1
    const val CLASS_KEYWORD: SyntaxId = TYPE_ALIAS_KEYWORD + 1
    const val THIS_KEYWORD: SyntaxId = CLASS_KEYWORD + 1
    const val SUPER_KEYWORD: SyntaxId = THIS_KEYWORD + 1
    const val VAL_KEYWORD: SyntaxId = SUPER_KEYWORD + 1
    const val VAR_KEYWORD: SyntaxId = VAL_KEYWORD + 1
    const val FUN_MODIFIER: SyntaxId = VAR_KEYWORD + 1
    const val FOR_KEYWORD: SyntaxId = FUN_MODIFIER + 1
    const val NULL_KEYWORD: SyntaxId = FOR_KEYWORD + 1
    const val TRUE_KEYWORD: SyntaxId = NULL_KEYWORD + 1
    const val FALSE_KEYWORD: SyntaxId = TRUE_KEYWORD + 1
    const val IS_KEYWORD: SyntaxId = FALSE_KEYWORD + 1
    const val IN_MODIFIER: SyntaxId = IS_KEYWORD + 1
    const val THROW_KEYWORD: SyntaxId = IN_MODIFIER + 1
    const val RETURN_KEYWORD: SyntaxId = THROW_KEYWORD + 1
    const val BREAK_KEYWORD: SyntaxId = RETURN_KEYWORD + 1
    const val CONTINUE_KEYWORD: SyntaxId = BREAK_KEYWORD + 1
    const val OBJECT_KEYWORD: SyntaxId = CONTINUE_KEYWORD + 1
    const val IF_KEYWORD: SyntaxId = OBJECT_KEYWORD + 1
    const val TRY_KEYWORD: SyntaxId = IF_KEYWORD + 1
    const val ELSE_KEYWORD: SyntaxId = TRY_KEYWORD + 1
    const val WHILE_KEYWORD: SyntaxId = ELSE_KEYWORD + 1
    const val DO_KEYWORD: SyntaxId = WHILE_KEYWORD + 1
    const val WHEN_KEYWORD: SyntaxId = DO_KEYWORD + 1
    const val INTERFACE_KEYWORD: SyntaxId = WHEN_KEYWORD + 1

    const val TYPEOF_KEYWORD: SyntaxId = INTERFACE_KEYWORD + 1

    const val AS_SAFE: SyntaxId = TYPEOF_KEYWORD + 1

    const val IDENTIFIER: SyntaxId = AS_SAFE + 1
    const val FIELD_IDENTIFIER: SyntaxId = IDENTIFIER + 1
    const val LBRACKET: SyntaxId = FIELD_IDENTIFIER + 1
    const val RBRACKET: SyntaxId = LBRACKET + 1
    const val LBRACE: SyntaxId = RBRACKET + 1
    const val RBRACE: SyntaxId = LBRACE + 1
    const val LPAR: SyntaxId = RBRACE + 1
    const val RPAR: SyntaxId = LPAR + 1
    const val DOT: SyntaxId = RPAR + 1
    const val PLUSPLUS: SyntaxId = DOT + 1
    const val MINUSMINUS: SyntaxId = PLUSPLUS + 1
    const val MUL: SyntaxId = MINUSMINUS + 1
    const val PLUS: SyntaxId = MUL + 1
    const val MINUS: SyntaxId = PLUS + 1
    const val EXCL: SyntaxId = MINUS + 1
    const val DIV: SyntaxId = EXCL + 1
    const val PERC: SyntaxId = DIV + 1
    const val LT: SyntaxId = PERC + 1
    const val GT: SyntaxId = LT + 1
    const val LTEQ: SyntaxId = GT + 1
    const val GTEQ: SyntaxId = LTEQ + 1
    const val EQEQEQ: SyntaxId = GTEQ + 1
    const val ARROW: SyntaxId = EQEQEQ + 1
    const val DOUBLE_ARROW: SyntaxId = ARROW + 1
    const val EXCLEQEQEQ: SyntaxId = DOUBLE_ARROW + 1
    const val EQEQ: SyntaxId = EXCLEQEQEQ + 1
    const val EXCLEQ: SyntaxId = EQEQ + 1
    const val EXCLEXCL: SyntaxId = EXCLEQ + 1
    const val ANDAND: SyntaxId = EXCLEXCL + 1
    const val AND: SyntaxId = ANDAND + 1
    const val OROR: SyntaxId = AND + 1
    const val SAFE_ACCESS: SyntaxId = OROR + 1
    const val ELVIS: SyntaxId = SAFE_ACCESS + 1
    const val QUEST: SyntaxId = ELVIS + 1
    const val COLONCOLON: SyntaxId = QUEST + 1
    const val COLON: SyntaxId = COLONCOLON + 1
    const val SEMICOLON: SyntaxId = COLON + 1
    const val DOUBLE_SEMICOLON: SyntaxId = SEMICOLON + 1
    const val RANGE: SyntaxId = DOUBLE_SEMICOLON + 1
    const val RANGE_UNTIL: SyntaxId = RANGE + 1
    const val EQ: SyntaxId = RANGE_UNTIL + 1
    const val MULTEQ: SyntaxId = EQ + 1
    const val DIVEQ: SyntaxId = MULTEQ + 1
    const val PERCEQ: SyntaxId = DIVEQ + 1
    const val PLUSEQ: SyntaxId = PERCEQ + 1
    const val MINUSEQ: SyntaxId = PLUSEQ + 1
    const val NOT_IN: SyntaxId = MINUSEQ + 1
    const val NOT_IS: SyntaxId = NOT_IN + 1
    const val HASH: SyntaxId = NOT_IS + 1
    const val AT: SyntaxId = HASH + 1
    const val COMMA: SyntaxId = AT + 1
    const val EOL_OR_SEMICOLON: SyntaxId = COMMA + 1
    const val FILE_KEYWORD: SyntaxId = EOL_OR_SEMICOLON + 1
    const val FIELD_KEYWORD: SyntaxId = FILE_KEYWORD + 1
    const val PROPERTY_KEYWORD: SyntaxId = FIELD_KEYWORD + 1
    const val RECEIVER_KEYWORD: SyntaxId = PROPERTY_KEYWORD + 1
    const val PARAM_KEYWORD: SyntaxId = RECEIVER_KEYWORD + 1
    const val SETPARAM_KEYWORD: SyntaxId = PARAM_KEYWORD + 1
    const val DELEGATE_KEYWORD: SyntaxId = SETPARAM_KEYWORD + 1
    const val IMPORT_KEYWORD: SyntaxId = DELEGATE_KEYWORD + 1
    const val WHERE_KEYWORD: SyntaxId = IMPORT_KEYWORD + 1
    const val BY_KEYWORD: SyntaxId = WHERE_KEYWORD + 1
    const val GET_KEYWORD: SyntaxId = BY_KEYWORD + 1
    const val SET_KEYWORD: SyntaxId = GET_KEYWORD + 1
    const val CONSTRUCTOR_KEYWORD: SyntaxId = SET_KEYWORD + 1
    const val INIT_KEYWORD: SyntaxId = CONSTRUCTOR_KEYWORD + 1
    const val CONTEXT_KEYWORD: SyntaxId = INIT_KEYWORD + 1
    const val CATCH_KEYWORD: SyntaxId = CONTEXT_KEYWORD + 1
    const val DYNAMIC_KEYWORD: SyntaxId = CATCH_KEYWORD + 1
    const val FINALLY_KEYWORD: SyntaxId = DYNAMIC_KEYWORD + 1

    const val ABSTRACT_MODIFIER: SyntaxId = FINALLY_KEYWORD + 1
    const val ENUM_MODIFIER: SyntaxId = ABSTRACT_MODIFIER + 1
    const val CONTRACT_MODIFIER: SyntaxId = ENUM_MODIFIER + 1
    const val OPEN_MODIFIER: SyntaxId = CONTRACT_MODIFIER + 1
    const val INNER_MODIFIER: SyntaxId = OPEN_MODIFIER + 1
    const val OVERRIDE_MODIFIER: SyntaxId = INNER_MODIFIER + 1
    const val PRIVATE_MODIFIER: SyntaxId = OVERRIDE_MODIFIER + 1
    const val PUBLIC_MODIFIER: SyntaxId = PRIVATE_MODIFIER + 1
    const val INTERNAL_MODIFIER: SyntaxId = PUBLIC_MODIFIER + 1
    const val PROTECTED_MODIFIER: SyntaxId = INTERNAL_MODIFIER + 1
    const val OUT_MODIFIER: SyntaxId = PROTECTED_MODIFIER + 1
    const val VARARG_MODIFIER: SyntaxId = OUT_MODIFIER + 1
    const val REIFIED_MODIFIER: SyntaxId = VARARG_MODIFIER + 1
    const val COMPANION_MODIFIER: SyntaxId = REIFIED_MODIFIER + 1
    const val SEALED_MODIFIER: SyntaxId = COMPANION_MODIFIER + 1
    const val FINAL_MODIFIER: SyntaxId = SEALED_MODIFIER + 1
    const val LATEINIT_MODIFIER: SyntaxId = FINAL_MODIFIER + 1
    const val DATA_MODIFIER: SyntaxId = LATEINIT_MODIFIER + 1
    const val VALUE_MODIFIER: SyntaxId = DATA_MODIFIER + 1
    const val INLINE_MODIFIER: SyntaxId = VALUE_MODIFIER + 1
    const val NOINLINE_MODIFIER: SyntaxId = INLINE_MODIFIER + 1
    const val TAILREC_MODIFIER: SyntaxId = NOINLINE_MODIFIER + 1
    const val EXTERNAL_MODIFIER: SyntaxId = TAILREC_MODIFIER + 1
    const val ANNOTATION_MODIFIER: SyntaxId = EXTERNAL_MODIFIER + 1
    const val CROSSINLINE_MODIFIER: SyntaxId = ANNOTATION_MODIFIER + 1
    const val OPERATOR_MODIFIER: SyntaxId = CROSSINLINE_MODIFIER + 1
    const val INFIX_MODIFIER: SyntaxId = OPERATOR_MODIFIER + 1
    const val CONST_MODIFIER: SyntaxId = INFIX_MODIFIER + 1
    const val SUSPEND_MODIFIER: SyntaxId = CONST_MODIFIER + 1
    const val EXPECT_MODIFIER: SyntaxId = SUSPEND_MODIFIER + 1
    const val ACTUAL_MODIFIER: SyntaxId = EXPECT_MODIFIER + 1
    const val INTERPOLATION_PREFIX: SyntaxId = ACTUAL_MODIFIER + 1
    const val ALL_KEYWORD: SyntaxId = INTERPOLATION_PREFIX + 1
    const val ERROR_MODIFIER: SyntaxId = ALL_KEYWORD + 1
    const val OR: SyntaxId = ERROR_MODIFIER + 1
    const val ERROR_SAFE_ACCESS: SyntaxId = OR + 1
    /**
     * Remember to update first const in [KtNodeTypeId] after adding new tokens here ^^^
     */
}
