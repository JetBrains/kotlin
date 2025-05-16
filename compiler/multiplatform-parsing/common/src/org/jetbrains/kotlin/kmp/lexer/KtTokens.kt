/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.lexer

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.SyntaxElementTypeSet
import com.intellij.platform.syntax.asSyntaxElementTypeSet
import com.intellij.platform.syntax.element.SyntaxTokenTypes
import com.intellij.platform.syntax.syntaxElementTypeSetOf
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

@ApiStatus.Experimental
object KtTokens : SyntaxElementTypesWithIds() {
    /**
     * The following constants are needed to force the compiler to generate a fast table / lookup switch on when expressions with multiple branches.
     * They should match corresponding element types.
     * As an example, see [SyntaxElementTypesWithIds.getElementTypeId] usages.
     */
    const val EOF_ID: Int = KDocTokens.KDOC_RPAR_ID + 1
    const val RESERVED_ID: Int = EOF_ID + 1
    const val BLOCK_COMMENT_ID: Int = RESERVED_ID + 1
    const val EOL_COMMENT_ID: Int = BLOCK_COMMENT_ID + 1
    const val SHEBANG_COMMENT_ID: Int = EOL_COMMENT_ID + 1
    const val DOC_COMMENT_ID: Int = SHEBANG_COMMENT_ID + 1

    const val WHITE_SPACE_ID: Int = DOC_COMMENT_ID + 1

    const val INTEGER_LITERAL_ID: Int = WHITE_SPACE_ID + 1
    const val FLOAT_LITERAL_ID: Int = INTEGER_LITERAL_ID + 1
    const val CHARACTER_LITERAL_ID: Int = FLOAT_LITERAL_ID + 1

    const val INTERPOLATION_PREFIX_ID: Int = CHARACTER_LITERAL_ID + 1
    const val CLOSING_QUOTE_ID: Int = INTERPOLATION_PREFIX_ID + 1
    const val OPEN_QUOTE_ID: Int = CLOSING_QUOTE_ID + 1
    const val REGULAR_STRING_PART_ID: Int = OPEN_QUOTE_ID + 1
    const val ESCAPE_SEQUENCE_ID: Int = REGULAR_STRING_PART_ID + 1
    const val SHORT_TEMPLATE_ENTRY_START_ID: Int = ESCAPE_SEQUENCE_ID + 1
    const val LONG_TEMPLATE_ENTRY_START_ID: Int = SHORT_TEMPLATE_ENTRY_START_ID + 1
    const val LONG_TEMPLATE_ENTRY_END_ID: Int = LONG_TEMPLATE_ENTRY_START_ID + 1
    const val DANGLING_NEWLINE_ID: Int = LONG_TEMPLATE_ENTRY_END_ID + 1

    const val PACKAGE_KEYWORD_ID: Int = DANGLING_NEWLINE_ID + 1
    const val AS_KEYWORD_ID: Int = PACKAGE_KEYWORD_ID + 1
    const val TYPE_ALIAS_KEYWORD_ID: Int = AS_KEYWORD_ID + 1
    const val CLASS_KEYWORD_ID: Int = TYPE_ALIAS_KEYWORD_ID + 1
    const val THIS_KEYWORD_ID: Int = CLASS_KEYWORD_ID + 1
    const val SUPER_KEYWORD_ID: Int = THIS_KEYWORD_ID + 1
    const val VAL_KEYWORD_ID: Int = SUPER_KEYWORD_ID + 1
    const val VAR_KEYWORD_ID: Int = VAL_KEYWORD_ID + 1
    const val FUN_MODIFIER_ID: Int = VAR_KEYWORD_ID + 1
    const val FOR_KEYWORD_ID: Int = FUN_MODIFIER_ID + 1
    const val NULL_KEYWORD_ID: Int = FOR_KEYWORD_ID + 1
    const val TRUE_KEYWORD_ID: Int = NULL_KEYWORD_ID + 1
    const val FALSE_KEYWORD_ID: Int = TRUE_KEYWORD_ID + 1
    const val IS_KEYWORD_ID: Int = FALSE_KEYWORD_ID + 1
    const val IN_MODIFIER_ID: Int = IS_KEYWORD_ID + 1
    const val THROW_KEYWORD_ID: Int = IN_MODIFIER_ID + 1
    const val RETURN_KEYWORD_ID: Int = THROW_KEYWORD_ID + 1
    const val BREAK_KEYWORD_ID: Int = RETURN_KEYWORD_ID + 1
    const val CONTINUE_KEYWORD_ID: Int = BREAK_KEYWORD_ID + 1
    const val OBJECT_KEYWORD_ID: Int = CONTINUE_KEYWORD_ID + 1
    const val IF_KEYWORD_ID: Int = OBJECT_KEYWORD_ID + 1
    const val TRY_KEYWORD_ID: Int = IF_KEYWORD_ID + 1
    const val ELSE_KEYWORD_ID: Int = TRY_KEYWORD_ID + 1
    const val WHILE_KEYWORD_ID: Int = ELSE_KEYWORD_ID + 1
    const val DO_KEYWORD_ID: Int = WHILE_KEYWORD_ID + 1
    const val WHEN_KEYWORD_ID: Int = DO_KEYWORD_ID + 1
    const val INTERFACE_KEYWORD_ID: Int = WHEN_KEYWORD_ID + 1

    const val TYPEOF_KEYWORD_ID: Int = INTERFACE_KEYWORD_ID + 1

    const val AS_SAFE_ID: Int = TYPEOF_KEYWORD_ID + 1

    const val IDENTIFIER_ID: Int = AS_SAFE_ID + 1
    const val FIELD_IDENTIFIER_ID: Int = IDENTIFIER_ID + 1
    const val LBRACKET_ID: Int = FIELD_IDENTIFIER_ID + 1
    const val RBRACKET_ID: Int = LBRACKET_ID + 1
    const val LBRACE_ID: Int = RBRACKET_ID + 1
    const val RBRACE_ID: Int = LBRACE_ID + 1
    const val LPAR_ID: Int = RBRACE_ID + 1
    const val RPAR_ID: Int = LPAR_ID + 1
    const val DOT_ID: Int = RPAR_ID + 1
    const val PLUSPLUS_ID: Int = DOT_ID + 1
    const val MINUSMINUS_ID: Int = PLUSPLUS_ID + 1
    const val MUL_ID: Int = MINUSMINUS_ID + 1
    const val PLUS_ID: Int = MUL_ID + 1
    const val MINUS_ID: Int = PLUS_ID + 1
    const val EXCL_ID: Int = MINUS_ID + 1
    const val DIV_ID: Int = EXCL_ID + 1
    const val PERC_ID: Int = DIV_ID + 1
    const val LT_ID: Int = PERC_ID + 1
    const val GT_ID: Int = LT_ID + 1
    const val LTEQ_ID: Int = GT_ID + 1
    const val GTEQ_ID: Int = LTEQ_ID + 1
    const val EQEQEQ_ID: Int = GTEQ_ID + 1
    const val ARROW_ID: Int = EQEQEQ_ID + 1
    const val DOUBLE_ARROW_ID: Int = ARROW_ID + 1
    const val EXCLEQEQEQ_ID: Int = DOUBLE_ARROW_ID + 1
    const val EQEQ_ID: Int = EXCLEQEQEQ_ID + 1
    const val EXCLEQ_ID: Int = EQEQ_ID + 1
    const val EXCLEXCL_ID: Int = EXCLEQ_ID + 1
    const val ANDAND_ID: Int = EXCLEXCL_ID + 1
    const val AND_ID: Int = ANDAND_ID + 1
    const val OROR_ID: Int = AND_ID + 1
    const val SAFE_ACCESS_ID: Int = OROR_ID + 1
    const val ELVIS_ID: Int = SAFE_ACCESS_ID + 1
    const val QUEST_ID: Int = ELVIS_ID + 1
    const val COLONCOLON_ID: Int = QUEST_ID + 1
    const val COLON_ID: Int = COLONCOLON_ID + 1
    const val SEMICOLON_ID: Int = COLON_ID + 1
    const val DOUBLE_SEMICOLON_ID: Int = SEMICOLON_ID + 1
    const val RANGE_ID: Int = DOUBLE_SEMICOLON_ID + 1
    const val RANGE_UNTIL_ID: Int = RANGE_ID + 1
    const val EQ_ID: Int = RANGE_UNTIL_ID + 1
    const val MULTEQ_ID: Int = EQ_ID + 1
    const val DIVEQ_ID: Int = MULTEQ_ID + 1
    const val PERCEQ_ID: Int = DIVEQ_ID + 1
    const val PLUSEQ_ID: Int = PERCEQ_ID + 1
    const val MINUSEQ_ID: Int = PLUSEQ_ID + 1
    const val NOT_IN_ID: Int = MINUSEQ_ID + 1
    const val NOT_IS_ID: Int = NOT_IN_ID + 1
    const val HASH_ID: Int = NOT_IS_ID + 1
    const val AT_ID: Int = HASH_ID + 1
    const val COMMA_ID: Int = AT_ID + 1
    const val EOL_OR_SEMICOLON_ID: Int = COMMA_ID + 1
    const val ALL_KEYWORD_ID: Int = EOL_OR_SEMICOLON_ID + 1
    const val FILE_KEYWORD_ID: Int = ALL_KEYWORD_ID + 1
    const val FIELD_KEYWORD_ID: Int = FILE_KEYWORD_ID + 1
    const val PROPERTY_KEYWORD_ID: Int = FIELD_KEYWORD_ID + 1
    const val RECEIVER_KEYWORD_ID: Int = PROPERTY_KEYWORD_ID + 1
    const val PARAM_KEYWORD_ID: Int = RECEIVER_KEYWORD_ID + 1
    const val SETPARAM_KEYWORD_ID: Int = PARAM_KEYWORD_ID + 1
    const val DELEGATE_KEYWORD_ID: Int = SETPARAM_KEYWORD_ID + 1
    const val IMPORT_KEYWORD_ID: Int = DELEGATE_KEYWORD_ID + 1
    const val WHERE_KEYWORD_ID: Int = IMPORT_KEYWORD_ID + 1
    const val BY_KEYWORD_ID: Int = WHERE_KEYWORD_ID + 1
    const val GET_KEYWORD_ID: Int = BY_KEYWORD_ID + 1
    const val SET_KEYWORD_ID: Int = GET_KEYWORD_ID + 1
    const val CONSTRUCTOR_KEYWORD_ID: Int = SET_KEYWORD_ID + 1
    const val INIT_KEYWORD_ID: Int = CONSTRUCTOR_KEYWORD_ID + 1
    const val CONTEXT_KEYWORD_ID: Int = INIT_KEYWORD_ID + 1
    const val ABSTRACT_MODIFIER_ID: Int = CONTEXT_KEYWORD_ID + 1
    const val ENUM_MODIFIER_ID: Int = ABSTRACT_MODIFIER_ID + 1
    const val CONTRACT_MODIFIER_ID: Int = ENUM_MODIFIER_ID + 1
    const val OPEN_MODIFIER_ID: Int = CONTRACT_MODIFIER_ID + 1
    const val INNER_MODIFIER_ID: Int = OPEN_MODIFIER_ID + 1
    const val OVERRIDE_MODIFIER_ID: Int = INNER_MODIFIER_ID + 1
    const val PRIVATE_MODIFIER_ID: Int = OVERRIDE_MODIFIER_ID + 1
    const val PUBLIC_MODIFIER_ID: Int = PRIVATE_MODIFIER_ID + 1
    const val INTERNAL_MODIFIER_ID: Int = PUBLIC_MODIFIER_ID + 1
    const val PROTECTED_MODIFIER_ID: Int = INTERNAL_MODIFIER_ID + 1
    const val CATCH_KEYWORD_ID: Int = PROTECTED_MODIFIER_ID + 1
    const val OUT_MODIFIER_ID: Int = CATCH_KEYWORD_ID + 1
    const val VARARG_MODIFIER_ID: Int = OUT_MODIFIER_ID + 1
    const val REIFIED_MODIFIER_ID: Int = VARARG_MODIFIER_ID + 1
    const val DYNAMIC_KEYWORD_ID: Int = REIFIED_MODIFIER_ID + 1
    const val COMPANION_MODIFIER_ID: Int = DYNAMIC_KEYWORD_ID + 1
    const val SEALED_MODIFIER_ID: Int = COMPANION_MODIFIER_ID + 1
    const val FINALLY_KEYWORD_ID: Int = SEALED_MODIFIER_ID + 1
    const val FINAL_MODIFIER_ID: Int = FINALLY_KEYWORD_ID + 1
    const val LATEINIT_MODIFIER_ID: Int = FINAL_MODIFIER_ID + 1
    const val DATA_MODIFIER_ID: Int = LATEINIT_MODIFIER_ID + 1
    const val VALUE_MODIFIER_ID: Int = DATA_MODIFIER_ID + 1
    const val INLINE_MODIFIER_ID: Int = VALUE_MODIFIER_ID + 1
    const val NOINLINE_MODIFIER_ID: Int = INLINE_MODIFIER_ID + 1
    const val TAILREC_MODIFIER_ID: Int = NOINLINE_MODIFIER_ID + 1
    const val EXTERNAL_MODIFIER_ID: Int = TAILREC_MODIFIER_ID + 1
    const val ANNOTATION_MODIFIER_ID: Int = EXTERNAL_MODIFIER_ID + 1
    const val CROSSINLINE_MODIFIER_ID: Int = ANNOTATION_MODIFIER_ID + 1
    const val OPERATOR_MODIFIER_ID: Int = CROSSINLINE_MODIFIER_ID + 1
    const val INFIX_MODIFIER_ID: Int = OPERATOR_MODIFIER_ID + 1
    const val CONST_MODIFIER_ID: Int = INFIX_MODIFIER_ID + 1
    const val SUSPEND_MODIFIER_ID: Int = CONST_MODIFIER_ID + 1
    const val EXPECT_MODIFIER_ID: Int = SUSPEND_MODIFIER_ID + 1
    const val ACTUAL_MODIFIER_ID: Int = EXPECT_MODIFIER_ID + 1
    // Remember to update the first ID constant in `KtNodeTypes` after adding a new token

    private val softKeywordsAndModifiers: MutableSet<SyntaxElementType> = mutableSetOf()
    private val hardKeywordsAndModifiers: MutableSet<SyntaxElementType> = mutableSetOf()
    private val allModifiers: MutableSet<SyntaxElementType> = mutableSetOf()

    val EOF: SyntaxElementType = register(EOF_ID, "EOF")

    val RESERVED: SyntaxElementType = register(RESERVED_ID, "RESERVED")

    val BLOCK_COMMENT: SyntaxElementType = register(BLOCK_COMMENT_ID, "BLOCK_COMMENT")
    val EOL_COMMENT: SyntaxElementType = register(EOL_COMMENT_ID, "EOL_COMMENT")
    val SHEBANG_COMMENT: SyntaxElementType = register(SHEBANG_COMMENT_ID, "SHEBANG_COMMENT")

    val DOC_COMMENT: SyntaxElementType = register(DOC_COMMENT_ID, "KDoc")

    val WHITE_SPACE: SyntaxElementType = SyntaxTokenTypes.WHITE_SPACE

    val INTEGER_LITERAL: SyntaxElementType = register(INTEGER_LITERAL_ID, "INTEGER_LITERAL")
    val FLOAT_LITERAL: SyntaxElementType = register(FLOAT_LITERAL_ID, "FLOAT_CONSTANT")
    val CHARACTER_LITERAL: SyntaxElementType = register(CHARACTER_LITERAL_ID, "CHARACTER_LITERAL")

    val INTERPOLATION_PREFIX: SyntaxElementType = register(INTERPOLATION_PREFIX_ID, "INTERPOLATION_PREFIX")
    val CLOSING_QUOTE: SyntaxElementType = register(CLOSING_QUOTE_ID, "CLOSING_QUOTE")
    val OPEN_QUOTE: SyntaxElementType = register(OPEN_QUOTE_ID, "OPEN_QUOTE")
    val REGULAR_STRING_PART: SyntaxElementType = register(REGULAR_STRING_PART_ID, "REGULAR_STRING_PART")
    val ESCAPE_SEQUENCE: SyntaxElementType = register(ESCAPE_SEQUENCE_ID, "ESCAPE_SEQUENCE")
    val SHORT_TEMPLATE_ENTRY_START: SyntaxElementType = register(SHORT_TEMPLATE_ENTRY_START_ID, "SHORT_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_START: SyntaxElementType = register(LONG_TEMPLATE_ENTRY_START_ID, "LONG_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_END: SyntaxElementType = register(LONG_TEMPLATE_ENTRY_END_ID, "LONG_TEMPLATE_ENTRY_END")
    val DANGLING_NEWLINE: SyntaxElementType = register(DANGLING_NEWLINE_ID, "DANGLING_NEWLINE")

    val PACKAGE_KEYWORD: SyntaxElementType = registerToken(PACKAGE_KEYWORD_ID, "package", soft = false, modifier = false)
    val AS_KEYWORD: SyntaxElementType = registerToken(AS_KEYWORD_ID, "as", soft = false, modifier = false)
    val TYPE_ALIAS_KEYWORD: SyntaxElementType = registerToken(TYPE_ALIAS_KEYWORD_ID, "typealias", soft = false, modifier = false)
    val CLASS_KEYWORD: SyntaxElementType = registerToken(CLASS_KEYWORD_ID, "class", soft = false, modifier = false)
    val THIS_KEYWORD: SyntaxElementType = registerToken(THIS_KEYWORD_ID, "this", soft = false, modifier = false)
    val SUPER_KEYWORD: SyntaxElementType = registerToken(SUPER_KEYWORD_ID, "super", soft = false, modifier = false)
    val VAL_KEYWORD: SyntaxElementType = registerToken(VAL_KEYWORD_ID, "val", soft = false, modifier = false)
    val VAR_KEYWORD: SyntaxElementType = registerToken(VAR_KEYWORD_ID, "var", soft = false, modifier = false)

    val FUN_MODIFIER: SyntaxElementType = registerToken(FUN_MODIFIER_ID, "fun", soft = false, modifier = true)

    val FOR_KEYWORD: SyntaxElementType = registerToken(FOR_KEYWORD_ID, "for", soft = false, modifier = false)
    val NULL_KEYWORD: SyntaxElementType = registerToken(NULL_KEYWORD_ID, "null", soft = false, modifier = false)
    val TRUE_KEYWORD: SyntaxElementType = registerToken(TRUE_KEYWORD_ID, "true", soft = false, modifier = false)
    val FALSE_KEYWORD: SyntaxElementType = registerToken(FALSE_KEYWORD_ID, "false", soft = false, modifier = false)
    val IS_KEYWORD: SyntaxElementType = registerToken(IS_KEYWORD_ID, "is", soft = false, modifier = false)

    val IN_MODIFIER: SyntaxElementType = registerToken(IN_MODIFIER_ID, "in", soft = false, modifier = true)

    val THROW_KEYWORD: SyntaxElementType = registerToken(THROW_KEYWORD_ID, "throw", soft = false, modifier = false)
    val RETURN_KEYWORD: SyntaxElementType = registerToken(RETURN_KEYWORD_ID, "return", soft = false, modifier = false)
    val BREAK_KEYWORD: SyntaxElementType = registerToken(BREAK_KEYWORD_ID, "break", soft = false, modifier = false)
    val CONTINUE_KEYWORD: SyntaxElementType = registerToken(CONTINUE_KEYWORD_ID, "continue", soft = false, modifier = false)
    val OBJECT_KEYWORD: SyntaxElementType = registerToken(OBJECT_KEYWORD_ID, "object", soft = false, modifier = false)
    val IF_KEYWORD: SyntaxElementType = registerToken(IF_KEYWORD_ID, "if", soft = false, modifier = false)
    val TRY_KEYWORD: SyntaxElementType = registerToken(TRY_KEYWORD_ID, "try", soft = false, modifier = false)
    val ELSE_KEYWORD: SyntaxElementType = registerToken(ELSE_KEYWORD_ID, "else", soft = false, modifier = false)
    val WHILE_KEYWORD: SyntaxElementType = registerToken(WHILE_KEYWORD_ID, "while", soft = false, modifier = false)
    val DO_KEYWORD: SyntaxElementType = registerToken(DO_KEYWORD_ID, "do", soft = false, modifier = false)
    val WHEN_KEYWORD: SyntaxElementType = registerToken(WHEN_KEYWORD_ID, "when", soft = false, modifier = false)
    val INTERFACE_KEYWORD: SyntaxElementType = registerToken(INTERFACE_KEYWORD_ID, "interface", soft = false, modifier = false)

    // Reserved for future use:
    val TYPEOF_KEYWORD: SyntaxElementType = registerToken(TYPEOF_KEYWORD_ID, "typeof", soft = false, modifier = false)

    val AS_SAFE: SyntaxElementType = registerToken(AS_SAFE_ID, "AS_SAFE", soft = false, modifier = false)

    val IDENTIFIER: SyntaxElementType = register(IDENTIFIER_ID, "IDENTIFIER")

    val FIELD_IDENTIFIER: SyntaxElementType = register(FIELD_IDENTIFIER_ID, "FIELD_IDENTIFIER")
    val LBRACKET: SyntaxElementType = register(LBRACKET_ID, "LBRACKET")
    val RBRACKET: SyntaxElementType = register(RBRACKET_ID, "RBRACKET")
    val LBRACE: SyntaxElementType = register(LBRACE_ID, "LBRACE")
    val RBRACE: SyntaxElementType = register(RBRACE_ID, "RBRACE")
    val LPAR: SyntaxElementType = register(LPAR_ID, "LPAR")
    val RPAR: SyntaxElementType = register(RPAR_ID, "RPAR")
    val DOT: SyntaxElementType = register(DOT_ID, "DOT")
    val PLUSPLUS: SyntaxElementType = register(PLUSPLUS_ID, "PLUSPLUS")
    val MINUSMINUS: SyntaxElementType = register(MINUSMINUS_ID, "MINUSMINUS")
    val MUL: SyntaxElementType = register(MUL_ID, "MUL")
    val PLUS: SyntaxElementType = register(PLUS_ID, "PLUS")
    val MINUS: SyntaxElementType = register(MINUS_ID, "MINUS")
    val EXCL: SyntaxElementType = register(EXCL_ID, "EXCL")
    val DIV: SyntaxElementType = register(DIV_ID, "DIV")
    val PERC: SyntaxElementType = register(PERC_ID, "PERC")
    val LT: SyntaxElementType = register(LT_ID, "LT")
    val GT: SyntaxElementType = register(GT_ID, "GT")
    val LTEQ: SyntaxElementType = register(LTEQ_ID, "LTEQ")
    val GTEQ: SyntaxElementType = register(GTEQ_ID, "GTEQ")
    val EQEQEQ: SyntaxElementType = register(EQEQEQ_ID, "EQEQEQ")
    val ARROW: SyntaxElementType = register(ARROW_ID, "ARROW")
    val DOUBLE_ARROW: SyntaxElementType = register(DOUBLE_ARROW_ID, "DOUBLE_ARROW")
    val EXCLEQEQEQ: SyntaxElementType = register(EXCLEQEQEQ_ID, "EXCLEQEQEQ")
    val EQEQ: SyntaxElementType = register(EQEQ_ID, "EQEQ")
    val EXCLEQ: SyntaxElementType = register(EXCLEQ_ID, "EXCLEQ")
    val EXCLEXCL: SyntaxElementType = register(EXCLEXCL_ID, "EXCLEXCL")
    val ANDAND: SyntaxElementType = register(ANDAND_ID, "ANDAND")
    val AND: SyntaxElementType = register(AND_ID, "AND")
    val OROR: SyntaxElementType = register(OROR_ID, "OROR")
    val SAFE_ACCESS: SyntaxElementType = register(SAFE_ACCESS_ID, "SAFE_ACCESS")
    val ELVIS: SyntaxElementType = register(ELVIS_ID, "ELVIS")
    val QUEST: SyntaxElementType = register(QUEST_ID, "QUEST")
    val COLONCOLON: SyntaxElementType = register(COLONCOLON_ID, "COLONCOLON")
    val COLON: SyntaxElementType = register(COLON_ID, "COLON")
    val SEMICOLON: SyntaxElementType = register(SEMICOLON_ID, "SEMICOLON")
    val DOUBLE_SEMICOLON: SyntaxElementType = register(DOUBLE_SEMICOLON_ID, "DOUBLE_SEMICOLON")
    val RANGE: SyntaxElementType = register(RANGE_ID, "RANGE")
    val RANGE_UNTIL: SyntaxElementType = register(RANGE_UNTIL_ID, "RANGE_UNTIL")
    val EQ: SyntaxElementType = register(EQ_ID, "EQ")
    val MULTEQ: SyntaxElementType = register(MULTEQ_ID, "MULTEQ")
    val DIVEQ: SyntaxElementType = register(DIVEQ_ID, "DIVEQ")
    val PERCEQ: SyntaxElementType = register(PERCEQ_ID, "PERCEQ")
    val PLUSEQ: SyntaxElementType = register(PLUSEQ_ID, "PLUSEQ")
    val MINUSEQ: SyntaxElementType = register(MINUSEQ_ID, "MINUSEQ")

    val NOT_IN: SyntaxElementType = registerToken(NOT_IN_ID, "NOT_IN", soft = false, modifier = false)
    val NOT_IS: SyntaxElementType = registerToken(NOT_IS_ID, "NOT_IS", soft = false, modifier = false)

    val HASH: SyntaxElementType = register(HASH_ID, "HASH")
    val AT: SyntaxElementType = register(AT_ID, "AT")

    val COMMA: SyntaxElementType = register(COMMA_ID, "COMMA")

    val EOL_OR_SEMICOLON: SyntaxElementType = register(EOL_OR_SEMICOLON_ID, "EOL_OR_SEMICOLON")

    val ALL_KEYWORD: SyntaxElementType = registerToken(ALL_KEYWORD_ID, "all", soft = true, modifier = false)
    val FILE_KEYWORD: SyntaxElementType = registerToken(FILE_KEYWORD_ID, "file", soft = true, modifier = false)
    val FIELD_KEYWORD: SyntaxElementType = registerToken(FIELD_KEYWORD_ID, "field", soft = true, modifier = false)
    val PROPERTY_KEYWORD: SyntaxElementType = registerToken(PROPERTY_KEYWORD_ID, "property", soft = true, modifier = false)
    val RECEIVER_KEYWORD: SyntaxElementType = registerToken(RECEIVER_KEYWORD_ID, "receiver", soft = true, modifier = false)
    val PARAM_KEYWORD: SyntaxElementType = registerToken(PARAM_KEYWORD_ID, "param", soft = true, modifier = false)
    val SETPARAM_KEYWORD: SyntaxElementType = registerToken(SETPARAM_KEYWORD_ID, "setparam", soft = true, modifier = false)
    val DELEGATE_KEYWORD: SyntaxElementType = registerToken(DELEGATE_KEYWORD_ID, "delegate", soft = true, modifier = false)
    val IMPORT_KEYWORD: SyntaxElementType = registerToken(IMPORT_KEYWORD_ID, "import", soft = true, modifier = false)
    val WHERE_KEYWORD: SyntaxElementType = registerToken(WHERE_KEYWORD_ID, "where", soft = true, modifier = false)
    val BY_KEYWORD: SyntaxElementType = registerToken(BY_KEYWORD_ID, "by", soft = true, modifier = false)
    val GET_KEYWORD: SyntaxElementType = registerToken(GET_KEYWORD_ID, "get", soft = true, modifier = false)
    val SET_KEYWORD: SyntaxElementType = registerToken(SET_KEYWORD_ID, "set", soft = true, modifier = false)
    val CONSTRUCTOR_KEYWORD: SyntaxElementType = registerToken(CONSTRUCTOR_KEYWORD_ID, "constructor", soft = true, modifier = false)
    val INIT_KEYWORD: SyntaxElementType = registerToken(INIT_KEYWORD_ID, "init", soft = true, modifier = false)
    val CONTEXT_KEYWORD: SyntaxElementType = registerToken(CONTEXT_KEYWORD_ID, "context", soft = true, modifier = false)

    val ABSTRACT_MODIFIER: SyntaxElementType = registerToken(ABSTRACT_MODIFIER_ID, "abstract", soft = true, modifier = true)
    val ENUM_MODIFIER: SyntaxElementType = registerToken(ENUM_MODIFIER_ID, "enum", soft = true, modifier = true)
    val CONTRACT_MODIFIER: SyntaxElementType = registerToken(CONTRACT_MODIFIER_ID, "contract", soft = true, modifier = true)
    val OPEN_MODIFIER: SyntaxElementType = registerToken(OPEN_MODIFIER_ID, "open", soft = true, modifier = true)
    val INNER_MODIFIER: SyntaxElementType = registerToken(INNER_MODIFIER_ID, "inner", soft = true, modifier = true)
    val OVERRIDE_MODIFIER: SyntaxElementType = registerToken(OVERRIDE_MODIFIER_ID, "override", soft = true, modifier = true)
    val PRIVATE_MODIFIER: SyntaxElementType = registerToken(PRIVATE_MODIFIER_ID, "private", soft = true, modifier = true)
    val PUBLIC_MODIFIER: SyntaxElementType = registerToken(PUBLIC_MODIFIER_ID, "public", soft = true, modifier = true)
    val INTERNAL_MODIFIER: SyntaxElementType = registerToken(INTERNAL_MODIFIER_ID, "internal", soft = true, modifier = true)
    val PROTECTED_MODIFIER: SyntaxElementType = registerToken(PROTECTED_MODIFIER_ID, "protected", soft = true, modifier = true)

    val CATCH_KEYWORD: SyntaxElementType = registerToken(CATCH_KEYWORD_ID, "catch", soft = true, modifier = false)

    val OUT_MODIFIER: SyntaxElementType = registerToken(OUT_MODIFIER_ID, "out", soft = true, modifier = true)
    val VARARG_MODIFIER: SyntaxElementType = registerToken(VARARG_MODIFIER_ID, "vararg", soft = true, modifier = true)
    val REIFIED_MODIFIER: SyntaxElementType = registerToken(REIFIED_MODIFIER_ID, "reified", soft = true, modifier = true)

    val DYNAMIC_KEYWORD: SyntaxElementType = registerToken(DYNAMIC_KEYWORD_ID, "dynamic", soft = true, modifier = false)

    val COMPANION_MODIFIER: SyntaxElementType = registerToken(COMPANION_MODIFIER_ID, "companion", soft = true, modifier = true)
    val SEALED_MODIFIER: SyntaxElementType = registerToken(SEALED_MODIFIER_ID, "sealed", soft = true, modifier = true)

    val FINALLY_KEYWORD: SyntaxElementType = registerToken(FINALLY_KEYWORD_ID, "finally", soft = true, modifier = false)
    val FINAL_MODIFIER: SyntaxElementType = registerToken(FINAL_MODIFIER_ID, "final", soft = true, modifier = true)

    val LATEINIT_MODIFIER: SyntaxElementType = registerToken(LATEINIT_MODIFIER_ID, "lateinit", soft = true, modifier = true)

    val DATA_MODIFIER: SyntaxElementType = registerToken(DATA_MODIFIER_ID, "data", soft = true, modifier = true)
    val VALUE_MODIFIER: SyntaxElementType = registerToken(VALUE_MODIFIER_ID, "value", soft = true, modifier = true)
    val INLINE_MODIFIER: SyntaxElementType = registerToken(INLINE_MODIFIER_ID, "inline", soft = true, modifier = true)
    val NOINLINE_MODIFIER: SyntaxElementType = registerToken(NOINLINE_MODIFIER_ID, "noinline", soft = true, modifier = true)
    val TAILREC_MODIFIER: SyntaxElementType = registerToken(TAILREC_MODIFIER_ID, "tailrec", soft = true, modifier = true)
    val EXTERNAL_MODIFIER: SyntaxElementType = registerToken(EXTERNAL_MODIFIER_ID, "external", soft = true, modifier = true)
    val ANNOTATION_MODIFIER: SyntaxElementType = registerToken(ANNOTATION_MODIFIER_ID, "annotation", soft = true, modifier = true)
    val CROSSINLINE_MODIFIER: SyntaxElementType = registerToken(CROSSINLINE_MODIFIER_ID, "crossinline", soft = true, modifier = true)
    val OPERATOR_MODIFIER: SyntaxElementType = registerToken(OPERATOR_MODIFIER_ID, "operator", soft = true, modifier = true)
    val INFIX_MODIFIER: SyntaxElementType = registerToken(INFIX_MODIFIER_ID, "infix", soft = true, modifier = true)

    val CONST_MODIFIER: SyntaxElementType = registerToken(CONST_MODIFIER_ID, "const", soft = true, modifier = true)

    val SUSPEND_MODIFIER: SyntaxElementType = registerToken(SUSPEND_MODIFIER_ID, "suspend", soft = true, modifier = true)

    val EXPECT_MODIFIER: SyntaxElementType = registerToken(EXPECT_MODIFIER_ID, "expect", soft = true, modifier = true)
    val ACTUAL_MODIFIER: SyntaxElementType = registerToken(ACTUAL_MODIFIER_ID, "actual", soft = true, modifier = true)

    private fun registerToken(id: Int, name: String, soft: Boolean, modifier: Boolean): SyntaxElementType {
        return register(id, name).also {
            val keywordsAndModifiersList = if (soft) softKeywordsAndModifiers else hardKeywordsAndModifiers
            keywordsAndModifiersList.add(it)
            if (modifier) {
                allModifiers.add(it)
            }
        }
    }

    val SOFT_KEYWORDS_AND_MODIFIERS: SyntaxElementTypeSet = softKeywordsAndModifiers.asSyntaxElementTypeSet()
    val HARD_KEYWORDS_AND_MODIFIERS: SyntaxElementTypeSet = hardKeywordsAndModifiers.asSyntaxElementTypeSet()
    val MODIFIERS: SyntaxElementTypeSet = allModifiers.asSyntaxElementTypeSet()

    private val HARD_KEYWORDS_AND_MODIFIERS_MAP: Map<String, SyntaxElementType> = HARD_KEYWORDS_AND_MODIFIERS.associateBy { it.toString() }

    private val SOFT_KEYWORDS_AND_MODIFIERS_MAP: Map<String, SyntaxElementType> = SOFT_KEYWORDS_AND_MODIFIERS.associateBy { it.toString() }

    fun getHardKeywordOrModifier(elementText: String?): SyntaxElementType? {
        return elementText?.let { HARD_KEYWORDS_AND_MODIFIERS_MAP[it] }
    }

    fun isSoftKeywordOrModifier(elementText: String?): Boolean = getSoftKeywordOrModifier(elementText) != null

    fun getSoftKeywordOrModifier(elementText: String?): SyntaxElementType? {
        return elementText?.let { SOFT_KEYWORDS_AND_MODIFIERS_MAP[it] }
    }

    val TYPE_MODIFIER_KEYWORDS: SyntaxElementTypeSet = syntaxElementTypeSetOf(SUSPEND_MODIFIER)
    val TYPE_ARGUMENT_MODIFIER_KEYWORDS: SyntaxElementTypeSet = syntaxElementTypeSetOf(IN_MODIFIER, OUT_MODIFIER)
    val RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS: SyntaxElementTypeSet = syntaxElementTypeSetOf(OUT_MODIFIER, VARARG_MODIFIER)

    val WHITESPACES: SyntaxElementTypeSet = syntaxElementTypeSetOf(SyntaxTokenTypes.WHITE_SPACE)

    /**
     * Don't add KDocTokens to COMMENTS SyntaxElementTypeSet, because it is used in KotlinParserDefinition.getCommentTokens(),
     * and therefore all COMMENTS tokens will be ignored by SyntaxBuilder.
     */
    val COMMENTS: SyntaxElementTypeSet = syntaxElementTypeSetOf(EOL_COMMENT, BLOCK_COMMENT, DOC_COMMENT, SHEBANG_COMMENT)
    val WHITE_SPACE_OR_COMMENT_BIT_SET: SyntaxElementTypeSet = COMMENTS + WHITESPACES
}