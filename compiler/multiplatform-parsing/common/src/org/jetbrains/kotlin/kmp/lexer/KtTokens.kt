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
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.syntax.SyntaxTokenIds

@ApiStatus.Experimental
object KtTokens : SyntaxElementTypesWithIds() {
    private val softKeywordsAndModifiers: MutableSet<SyntaxElementType> = mutableSetOf()
    private val hardKeywordsAndModifiers: MutableSet<SyntaxElementType> = mutableSetOf()
    private val allModifiers: MutableSet<SyntaxElementType> = mutableSetOf()

    val EOF: SyntaxElementType = register(SyntaxTokenIds.EOF, "EOF")

    val RESERVED: SyntaxElementType = register(SyntaxTokenIds.RESERVED, "RESERVED")

    val BLOCK_COMMENT: SyntaxElementType = register(SyntaxTokenIds.BLOCK_COMMENT, "BLOCK_COMMENT")
    val EOL_COMMENT: SyntaxElementType = register(SyntaxTokenIds.EOL_COMMENT, "EOL_COMMENT")
    val SHEBANG_COMMENT: SyntaxElementType = register(SyntaxTokenIds.SHEBANG_COMMENT, "SHEBANG_COMMENT")

    val DOC_COMMENT: SyntaxElementType = register(SyntaxTokenIds.DOC_COMMENT, "KDoc")

    val INTEGER_LITERAL: SyntaxElementType = register(SyntaxTokenIds.INTEGER_LITERAL, "INTEGER_LITERAL")
    val FLOAT_LITERAL: SyntaxElementType = register(SyntaxTokenIds.FLOAT_LITERAL, "FLOAT_CONSTANT")
    val CHARACTER_LITERAL: SyntaxElementType = register(SyntaxTokenIds.CHARACTER_LITERAL, "CHARACTER_LITERAL")

    val INTERPOLATION_PREFIX: SyntaxElementType = register(SyntaxTokenIds.INTERPOLATION_PREFIX, "INTERPOLATION_PREFIX")
    val CLOSING_QUOTE: SyntaxElementType = register(SyntaxTokenIds.CLOSING_QUOTE, "CLOSING_QUOTE")
    val OPEN_QUOTE: SyntaxElementType = register(SyntaxTokenIds.OPEN_QUOTE, "OPEN_QUOTE")
    val REGULAR_STRING_PART: SyntaxElementType = register(SyntaxTokenIds.REGULAR_STRING_PART, "REGULAR_STRING_PART")
    val ESCAPE_SEQUENCE: SyntaxElementType = register(SyntaxTokenIds.ESCAPE_SEQUENCE, "ESCAPE_SEQUENCE")
    val SHORT_TEMPLATE_ENTRY_START: SyntaxElementType = register(SyntaxTokenIds.SHORT_TEMPLATE_ENTRY_START, "SHORT_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_START: SyntaxElementType = register(SyntaxTokenIds.LONG_TEMPLATE_ENTRY_START, "LONG_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_END: SyntaxElementType = register(SyntaxTokenIds.LONG_TEMPLATE_ENTRY_END, "LONG_TEMPLATE_ENTRY_END")
    val DANGLING_NEWLINE: SyntaxElementType = register(SyntaxTokenIds.DANGLING_NEWLINE, "DANGLING_NEWLINE")

    val PACKAGE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.PACKAGE_KEYWORD, "package", soft = false, modifier = false)
    val AS_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.AS_KEYWORD, "as", soft = false, modifier = false)
    val TYPE_ALIAS_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.TYPE_ALIAS_KEYWORD, "typealias", soft = false, modifier = false)
    val CLASS_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.CLASS_KEYWORD, "class", soft = false, modifier = false)
    val THIS_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.THIS_KEYWORD, "this", soft = false, modifier = false)
    val SUPER_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.SUPER_KEYWORD, "super", soft = false, modifier = false)
    val VAL_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.VAL_KEYWORD, "val", soft = false, modifier = false)
    val VAR_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.VAR_KEYWORD, "var", soft = false, modifier = false)

    val FUN_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.FUN_MODIFIER, "fun", soft = false, modifier = true)

    val FOR_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.FOR_KEYWORD, "for", soft = false, modifier = false)
    val NULL_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.NULL_KEYWORD, "null", soft = false, modifier = false)
    val TRUE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.TRUE_KEYWORD, "true", soft = false, modifier = false)
    val FALSE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.FALSE_KEYWORD, "false", soft = false, modifier = false)
    val IS_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.IS_KEYWORD, "is", soft = false, modifier = false)

    val IN_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.IN_MODIFIER, "in", soft = false, modifier = true)

    val THROW_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.THROW_KEYWORD, "throw", soft = false, modifier = false)
    val RETURN_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.RETURN_KEYWORD, "return", soft = false, modifier = false)
    val BREAK_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.BREAK_KEYWORD, "break", soft = false, modifier = false)
    val CONTINUE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.CONTINUE_KEYWORD, "continue", soft = false, modifier = false)
    val OBJECT_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.OBJECT_KEYWORD, "object", soft = false, modifier = false)
    val IF_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.IF_KEYWORD, "if", soft = false, modifier = false)
    val TRY_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.TRY_KEYWORD, "try", soft = false, modifier = false)
    val ELSE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.ELSE_KEYWORD, "else", soft = false, modifier = false)
    val WHILE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.WHILE_KEYWORD, "while", soft = false, modifier = false)
    val DO_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.DO_KEYWORD, "do", soft = false, modifier = false)
    val WHEN_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.WHEN_KEYWORD, "when", soft = false, modifier = false)
    val INTERFACE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.INTERFACE_KEYWORD, "interface", soft = false, modifier = false)

    // Reserved for future use:
    val TYPEOF_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.TYPEOF_KEYWORD, "typeof", soft = false, modifier = false)

    val AS_SAFE: SyntaxElementType = registerToken(SyntaxTokenIds.AS_SAFE, "AS_SAFE", soft = false, modifier = false)

    val IDENTIFIER: SyntaxElementType = register(SyntaxTokenIds.IDENTIFIER, "IDENTIFIER")

    val FIELD_IDENTIFIER: SyntaxElementType = register(SyntaxTokenIds.FIELD_IDENTIFIER, "FIELD_IDENTIFIER")
    val LBRACKET: SyntaxElementType = register(SyntaxTokenIds.LBRACKET, "LBRACKET")
    val RBRACKET: SyntaxElementType = register(SyntaxTokenIds.RBRACKET, "RBRACKET")
    val LBRACE: SyntaxElementType = register(SyntaxTokenIds.LBRACE, "LBRACE")
    val RBRACE: SyntaxElementType = register(SyntaxTokenIds.RBRACE, "RBRACE")
    val LPAR: SyntaxElementType = register(SyntaxTokenIds.LPAR, "LPAR")
    val RPAR: SyntaxElementType = register(SyntaxTokenIds.RPAR, "RPAR")
    val DOT: SyntaxElementType = register(SyntaxTokenIds.DOT, "DOT")
    val PLUSPLUS: SyntaxElementType = register(SyntaxTokenIds.PLUSPLUS, "PLUSPLUS")
    val MINUSMINUS: SyntaxElementType = register(SyntaxTokenIds.MINUSMINUS, "MINUSMINUS")
    val MUL: SyntaxElementType = register(SyntaxTokenIds.MUL, "MUL")
    val PLUS: SyntaxElementType = register(SyntaxTokenIds.PLUS, "PLUS")
    val MINUS: SyntaxElementType = register(SyntaxTokenIds.MINUS, "MINUS")
    val EXCL: SyntaxElementType = register(SyntaxTokenIds.EXCL, "EXCL")
    val DIV: SyntaxElementType = register(SyntaxTokenIds.DIV, "DIV")
    val PERC: SyntaxElementType = register(SyntaxTokenIds.PERC, "PERC")
    val LT: SyntaxElementType = register(SyntaxTokenIds.LT, "LT")
    val GT: SyntaxElementType = register(SyntaxTokenIds.GT, "GT")
    val LTEQ: SyntaxElementType = register(SyntaxTokenIds.LTEQ, "LTEQ")
    val GTEQ: SyntaxElementType = register(SyntaxTokenIds.GTEQ, "GTEQ")
    val EQEQEQ: SyntaxElementType = register(SyntaxTokenIds.EQEQEQ, "EQEQEQ")
    val ARROW: SyntaxElementType = register(SyntaxTokenIds.ARROW, "ARROW")
    val DOUBLE_ARROW: SyntaxElementType = register(SyntaxTokenIds.DOUBLE_ARROW, "DOUBLE_ARROW")
    val EXCLEQEQEQ: SyntaxElementType = register(SyntaxTokenIds.EXCLEQEQEQ, "EXCLEQEQEQ")
    val EQEQ: SyntaxElementType = register(SyntaxTokenIds.EQEQ, "EQEQ")
    val EXCLEQ: SyntaxElementType = register(SyntaxTokenIds.EXCLEQ, "EXCLEQ")
    val EXCLEXCL: SyntaxElementType = register(SyntaxTokenIds.EXCLEXCL, "EXCLEXCL")
    val ANDAND: SyntaxElementType = register(SyntaxTokenIds.ANDAND, "ANDAND")
    val AND: SyntaxElementType = register(SyntaxTokenIds.AND, "AND")
    val OROR: SyntaxElementType = register(SyntaxTokenIds.OROR, "OROR")
    val OR: SyntaxElementType = register(SyntaxTokenIds.OR, "OR")
    val SAFE_ACCESS: SyntaxElementType = register(SyntaxTokenIds.SAFE_ACCESS, "SAFE_ACCESS")
    val ERROR_SAFE_ACCESS: SyntaxElementType = register(SyntaxTokenIds.ERROR_SAFE_ACCESS, "ERROR_SAFE_ACCESS")
    val ELVIS: SyntaxElementType = register(SyntaxTokenIds.ELVIS, "ELVIS")
    val QUEST: SyntaxElementType = register(SyntaxTokenIds.QUEST, "QUEST")
    val COLONCOLON: SyntaxElementType = register(SyntaxTokenIds.COLONCOLON, "COLONCOLON")
    val COLON: SyntaxElementType = register(SyntaxTokenIds.COLON, "COLON")
    val SEMICOLON: SyntaxElementType = register(SyntaxTokenIds.SEMICOLON, "SEMICOLON")
    val DOUBLE_SEMICOLON: SyntaxElementType = register(SyntaxTokenIds.DOUBLE_SEMICOLON, "DOUBLE_SEMICOLON")
    val RANGE: SyntaxElementType = register(SyntaxTokenIds.RANGE, "RANGE")
    val RANGE_UNTIL: SyntaxElementType = register(SyntaxTokenIds.RANGE_UNTIL, "RANGE_UNTIL")
    val EQ: SyntaxElementType = register(SyntaxTokenIds.EQ, "EQ")
    val MULTEQ: SyntaxElementType = register(SyntaxTokenIds.MULTEQ, "MULTEQ")
    val DIVEQ: SyntaxElementType = register(SyntaxTokenIds.DIVEQ, "DIVEQ")
    val PERCEQ: SyntaxElementType = register(SyntaxTokenIds.PERCEQ, "PERCEQ")
    val PLUSEQ: SyntaxElementType = register(SyntaxTokenIds.PLUSEQ, "PLUSEQ")
    val MINUSEQ: SyntaxElementType = register(SyntaxTokenIds.MINUSEQ, "MINUSEQ")

    val NOT_IN: SyntaxElementType = registerToken(SyntaxTokenIds.NOT_IN, "NOT_IN", soft = false, modifier = false)
    val NOT_IS: SyntaxElementType = registerToken(SyntaxTokenIds.NOT_IS, "NOT_IS", soft = false, modifier = false)

    val HASH: SyntaxElementType = register(SyntaxTokenIds.HASH, "HASH")
    val AT: SyntaxElementType = register(SyntaxTokenIds.AT, "AT")

    val COMMA: SyntaxElementType = register(SyntaxTokenIds.COMMA, "COMMA")

    val EOL_OR_SEMICOLON: SyntaxElementType = register(SyntaxTokenIds.EOL_OR_SEMICOLON, "EOL_OR_SEMICOLON")

    val ALL_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.ALL_KEYWORD, "all", soft = true, modifier = false)
    val FILE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.FILE_KEYWORD, "file", soft = true, modifier = false)
    val FIELD_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.FIELD_KEYWORD, "field", soft = true, modifier = false)
    val PROPERTY_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.PROPERTY_KEYWORD, "property", soft = true, modifier = false)
    val RECEIVER_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.RECEIVER_KEYWORD, "receiver", soft = true, modifier = false)
    val PARAM_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.PARAM_KEYWORD, "param", soft = true, modifier = false)
    val SETPARAM_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.SETPARAM_KEYWORD, "setparam", soft = true, modifier = false)
    val DELEGATE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.DELEGATE_KEYWORD, "delegate", soft = true, modifier = false)
    val IMPORT_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.IMPORT_KEYWORD, "import", soft = true, modifier = false)
    val WHERE_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.WHERE_KEYWORD, "where", soft = true, modifier = false)
    val BY_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.BY_KEYWORD, "by", soft = true, modifier = false)
    val GET_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.GET_KEYWORD, "get", soft = true, modifier = false)
    val SET_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.SET_KEYWORD, "set", soft = true, modifier = false)
    val CONSTRUCTOR_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.CONSTRUCTOR_KEYWORD, "constructor", soft = true, modifier = false)
    val INIT_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.INIT_KEYWORD, "init", soft = true, modifier = false)
    val CONTEXT_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.CONTEXT_KEYWORD, "context", soft = true, modifier = false)

    val ABSTRACT_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.ABSTRACT_MODIFIER, "abstract", soft = true, modifier = true)
    val ENUM_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.ENUM_MODIFIER, "enum", soft = true, modifier = true)
    val CONTRACT_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.CONTRACT_MODIFIER, "contract", soft = true, modifier = true)
    val OPEN_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.OPEN_MODIFIER, "open", soft = true, modifier = true)
    val INNER_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.INNER_MODIFIER, "inner", soft = true, modifier = true)
    val OVERRIDE_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.OVERRIDE_MODIFIER, "override", soft = true, modifier = true)
    val PRIVATE_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.PRIVATE_MODIFIER, "private", soft = true, modifier = true)
    val PUBLIC_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.PUBLIC_MODIFIER, "public", soft = true, modifier = true)
    val INTERNAL_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.INTERNAL_MODIFIER, "internal", soft = true, modifier = true)
    val PROTECTED_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.PROTECTED_MODIFIER, "protected", soft = true, modifier = true)

    val CATCH_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.CATCH_KEYWORD, "catch", soft = true, modifier = false)

    val OUT_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.OUT_MODIFIER, "out", soft = true, modifier = true)
    val VARARG_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.VARARG_MODIFIER, "vararg", soft = true, modifier = true)
    val REIFIED_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.REIFIED_MODIFIER, "reified", soft = true, modifier = true)

    val DYNAMIC_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.DYNAMIC_KEYWORD, "dynamic", soft = true, modifier = false)

    val COMPANION_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.COMPANION_MODIFIER, "companion", soft = true, modifier = true)
    val SEALED_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.SEALED_MODIFIER, "sealed", soft = true, modifier = true)

    val FINALLY_KEYWORD: SyntaxElementType = registerToken(SyntaxTokenIds.FINALLY_KEYWORD, "finally", soft = true, modifier = false)
    val FINAL_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.FINAL_MODIFIER, "final", soft = true, modifier = true)

    val LATEINIT_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.LATEINIT_MODIFIER, "lateinit", soft = true, modifier = true)

    val DATA_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.DATA_MODIFIER, "data", soft = true, modifier = true)
    val VALUE_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.VALUE_MODIFIER, "value", soft = true, modifier = true)
    val INLINE_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.INLINE_MODIFIER, "inline", soft = true, modifier = true)
    val NOINLINE_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.NOINLINE_MODIFIER, "noinline", soft = true, modifier = true)
    val TAILREC_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.TAILREC_MODIFIER, "tailrec", soft = true, modifier = true)
    val EXTERNAL_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.EXTERNAL_MODIFIER, "external", soft = true, modifier = true)
    val ANNOTATION_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.ANNOTATION_MODIFIER, "annotation", soft = true, modifier = true)
    val CROSSINLINE_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.CROSSINLINE_MODIFIER, "crossinline", soft = true, modifier = true)
    val OPERATOR_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.OPERATOR_MODIFIER, "operator", soft = true, modifier = true)
    val INFIX_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.INFIX_MODIFIER, "infix", soft = true, modifier = true)
    val ERROR_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.ERROR_MODIFIER, "error", soft = true, modifier = true)

    val CONST_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.CONST_MODIFIER, "const", soft = true, modifier = true)

    val SUSPEND_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.SUSPEND_MODIFIER, "suspend", soft = true, modifier = true)

    val EXPECT_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.EXPECT_MODIFIER, "expect", soft = true, modifier = true)
    val ACTUAL_MODIFIER: SyntaxElementType = registerToken(SyntaxTokenIds.ACTUAL_MODIFIER, "actual", soft = true, modifier = true)

    private fun registerToken(id: SyntaxId, name: String, soft: Boolean, modifier: Boolean): SyntaxElementType {
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

    val HARD_KEYWORDS_AND_MODIFIERS_MAP: Map<String, SyntaxElementType> = HARD_KEYWORDS_AND_MODIFIERS.associateBy { it.toString() }

    val SOFT_KEYWORDS_AND_MODIFIERS_MAP: Map<String, SyntaxElementType> = SOFT_KEYWORDS_AND_MODIFIERS.associateBy { it.toString() }

    fun getHardKeywordOrModifier(elementText: String?): SyntaxElementType? {
        return elementText?.let { HARD_KEYWORDS_AND_MODIFIERS_MAP[it] }
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

    val VAL_VAR: SyntaxElementTypeSet = syntaxElementTypeSetOf(VAL_KEYWORD, VAR_KEYWORD)
}
