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
import org.jetbrains.kotlin.KtTokenId
import org.jetbrains.kotlin.SyntaxId
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

@ApiStatus.Experimental
object KtTokens : SyntaxElementTypesWithIds() {
    private val softKeywordsAndModifiers: MutableSet<SyntaxElementType> = mutableSetOf()
    private val hardKeywordsAndModifiers: MutableSet<SyntaxElementType> = mutableSetOf()
    private val allModifiers: MutableSet<SyntaxElementType> = mutableSetOf()

    val EOF: SyntaxElementType = register(KtTokenId.EOF, "EOF")

    val RESERVED: SyntaxElementType = register(KtTokenId.RESERVED, "RESERVED")

    val BLOCK_COMMENT: SyntaxElementType = register(KtTokenId.BLOCK_COMMENT, "BLOCK_COMMENT")
    val EOL_COMMENT: SyntaxElementType = register(KtTokenId.EOL_COMMENT, "EOL_COMMENT")
    val SHEBANG_COMMENT: SyntaxElementType = register(KtTokenId.SHEBANG_COMMENT, "SHEBANG_COMMENT")

    val DOC_COMMENT: SyntaxElementType = register(KtTokenId.DOC_COMMENT, "KDoc")

    val INTEGER_LITERAL: SyntaxElementType = register(KtTokenId.INTEGER_LITERAL, "INTEGER_LITERAL")
    val FLOAT_LITERAL: SyntaxElementType = register(KtTokenId.FLOAT_LITERAL, "FLOAT_CONSTANT")
    val CHARACTER_LITERAL: SyntaxElementType = register(KtTokenId.CHARACTER_LITERAL, "CHARACTER_LITERAL")

    val INTERPOLATION_PREFIX: SyntaxElementType = register(KtTokenId.INTERPOLATION_PREFIX, "INTERPOLATION_PREFIX")
    val CLOSING_QUOTE: SyntaxElementType = register(KtTokenId.CLOSING_QUOTE, "CLOSING_QUOTE")
    val OPEN_QUOTE: SyntaxElementType = register(KtTokenId.OPEN_QUOTE, "OPEN_QUOTE")
    val REGULAR_STRING_PART: SyntaxElementType = register(KtTokenId.REGULAR_STRING_PART, "REGULAR_STRING_PART")
    val ESCAPE_SEQUENCE: SyntaxElementType = register(KtTokenId.ESCAPE_SEQUENCE, "ESCAPE_SEQUENCE")
    val SHORT_TEMPLATE_ENTRY_START: SyntaxElementType = register(KtTokenId.SHORT_TEMPLATE_ENTRY_START, "SHORT_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_START: SyntaxElementType = register(KtTokenId.LONG_TEMPLATE_ENTRY_START, "LONG_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_END: SyntaxElementType = register(KtTokenId.LONG_TEMPLATE_ENTRY_END, "LONG_TEMPLATE_ENTRY_END")
    val DANGLING_NEWLINE: SyntaxElementType = register(KtTokenId.DANGLING_NEWLINE, "DANGLING_NEWLINE")

    val PACKAGE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.PACKAGE_KEYWORD, "package", soft = false, modifier = false)
    val AS_KEYWORD: SyntaxElementType = registerToken(KtTokenId.AS_KEYWORD, "as", soft = false, modifier = false)
    val TYPE_ALIAS_KEYWORD: SyntaxElementType = registerToken(KtTokenId.TYPE_ALIAS_KEYWORD, "typealias", soft = false, modifier = false)
    val CLASS_KEYWORD: SyntaxElementType = registerToken(KtTokenId.CLASS_KEYWORD, "class", soft = false, modifier = false)
    val THIS_KEYWORD: SyntaxElementType = registerToken(KtTokenId.THIS_KEYWORD, "this", soft = false, modifier = false)
    val SUPER_KEYWORD: SyntaxElementType = registerToken(KtTokenId.SUPER_KEYWORD, "super", soft = false, modifier = false)
    val VAL_KEYWORD: SyntaxElementType = registerToken(KtTokenId.VAL_KEYWORD, "val", soft = false, modifier = false)
    val VAR_KEYWORD: SyntaxElementType = registerToken(KtTokenId.VAR_KEYWORD, "var", soft = false, modifier = false)

    val FUN_MODIFIER: SyntaxElementType = registerToken(KtTokenId.FUN_MODIFIER, "fun", soft = false, modifier = true)

    val FOR_KEYWORD: SyntaxElementType = registerToken(KtTokenId.FOR_KEYWORD, "for", soft = false, modifier = false)
    val NULL_KEYWORD: SyntaxElementType = registerToken(KtTokenId.NULL_KEYWORD, "null", soft = false, modifier = false)
    val TRUE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.TRUE_KEYWORD, "true", soft = false, modifier = false)
    val FALSE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.FALSE_KEYWORD, "false", soft = false, modifier = false)
    val IS_KEYWORD: SyntaxElementType = registerToken(KtTokenId.IS_KEYWORD, "is", soft = false, modifier = false)

    val IN_MODIFIER: SyntaxElementType = registerToken(KtTokenId.IN_MODIFIER, "in", soft = false, modifier = true)

    val THROW_KEYWORD: SyntaxElementType = registerToken(KtTokenId.THROW_KEYWORD, "throw", soft = false, modifier = false)
    val RETURN_KEYWORD: SyntaxElementType = registerToken(KtTokenId.RETURN_KEYWORD, "return", soft = false, modifier = false)
    val BREAK_KEYWORD: SyntaxElementType = registerToken(KtTokenId.BREAK_KEYWORD, "break", soft = false, modifier = false)
    val CONTINUE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.CONTINUE_KEYWORD, "continue", soft = false, modifier = false)
    val OBJECT_KEYWORD: SyntaxElementType = registerToken(KtTokenId.OBJECT_KEYWORD, "object", soft = false, modifier = false)
    val IF_KEYWORD: SyntaxElementType = registerToken(KtTokenId.IF_KEYWORD, "if", soft = false, modifier = false)
    val TRY_KEYWORD: SyntaxElementType = registerToken(KtTokenId.TRY_KEYWORD, "try", soft = false, modifier = false)
    val ELSE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.ELSE_KEYWORD, "else", soft = false, modifier = false)
    val WHILE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.WHILE_KEYWORD, "while", soft = false, modifier = false)
    val DO_KEYWORD: SyntaxElementType = registerToken(KtTokenId.DO_KEYWORD, "do", soft = false, modifier = false)
    val WHEN_KEYWORD: SyntaxElementType = registerToken(KtTokenId.WHEN_KEYWORD, "when", soft = false, modifier = false)
    val INTERFACE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.INTERFACE_KEYWORD, "interface", soft = false, modifier = false)

    // Reserved for future use:
    val TYPEOF_KEYWORD: SyntaxElementType = registerToken(KtTokenId.TYPEOF_KEYWORD, "typeof", soft = false, modifier = false)

    val AS_SAFE: SyntaxElementType = registerToken(KtTokenId.AS_SAFE, "AS_SAFE", soft = false, modifier = false)

    val IDENTIFIER: SyntaxElementType = register(KtTokenId.IDENTIFIER, "IDENTIFIER")

    val FIELD_IDENTIFIER: SyntaxElementType = register(KtTokenId.FIELD_IDENTIFIER, "FIELD_IDENTIFIER")
    val LBRACKET: SyntaxElementType = register(KtTokenId.LBRACKET, "LBRACKET")
    val RBRACKET: SyntaxElementType = register(KtTokenId.RBRACKET, "RBRACKET")
    val LBRACE: SyntaxElementType = register(KtTokenId.LBRACE, "LBRACE")
    val RBRACE: SyntaxElementType = register(KtTokenId.RBRACE, "RBRACE")
    val LPAR: SyntaxElementType = register(KtTokenId.LPAR, "LPAR")
    val RPAR: SyntaxElementType = register(KtTokenId.RPAR, "RPAR")
    val DOT: SyntaxElementType = register(KtTokenId.DOT, "DOT")
    val PLUSPLUS: SyntaxElementType = register(KtTokenId.PLUSPLUS, "PLUSPLUS")
    val MINUSMINUS: SyntaxElementType = register(KtTokenId.MINUSMINUS, "MINUSMINUS")
    val MUL: SyntaxElementType = register(KtTokenId.MUL, "MUL")
    val PLUS: SyntaxElementType = register(KtTokenId.PLUS, "PLUS")
    val MINUS: SyntaxElementType = register(KtTokenId.MINUS, "MINUS")
    val EXCL: SyntaxElementType = register(KtTokenId.EXCL, "EXCL")
    val DIV: SyntaxElementType = register(KtTokenId.DIV, "DIV")
    val PERC: SyntaxElementType = register(KtTokenId.PERC, "PERC")
    val LT: SyntaxElementType = register(KtTokenId.LT, "LT")
    val GT: SyntaxElementType = register(KtTokenId.GT, "GT")
    val LTEQ: SyntaxElementType = register(KtTokenId.LTEQ, "LTEQ")
    val GTEQ: SyntaxElementType = register(KtTokenId.GTEQ, "GTEQ")
    val EQEQEQ: SyntaxElementType = register(KtTokenId.EQEQEQ, "EQEQEQ")
    val ARROW: SyntaxElementType = register(KtTokenId.ARROW, "ARROW")
    val DOUBLE_ARROW: SyntaxElementType = register(KtTokenId.DOUBLE_ARROW, "DOUBLE_ARROW")
    val EXCLEQEQEQ: SyntaxElementType = register(KtTokenId.EXCLEQEQEQ, "EXCLEQEQEQ")
    val EQEQ: SyntaxElementType = register(KtTokenId.EQEQ, "EQEQ")
    val EXCLEQ: SyntaxElementType = register(KtTokenId.EXCLEQ, "EXCLEQ")
    val EXCLEXCL: SyntaxElementType = register(KtTokenId.EXCLEXCL, "EXCLEXCL")
    val ANDAND: SyntaxElementType = register(KtTokenId.ANDAND, "ANDAND")
    val AND: SyntaxElementType = register(KtTokenId.AND, "AND")
    val OROR: SyntaxElementType = register(KtTokenId.OROR, "OROR")
    val OR: SyntaxElementType = register(KtTokenId.OR, "OR")
    val SAFE_ACCESS: SyntaxElementType = register(KtTokenId.SAFE_ACCESS, "SAFE_ACCESS")
    val ERROR_SAFE_ACCESS: SyntaxElementType = register(KtTokenId.ERROR_SAFE_ACCESS, "ERROR_SAFE_ACCESS")
    val ELVIS: SyntaxElementType = register(KtTokenId.ELVIS, "ELVIS")
    val QUEST: SyntaxElementType = register(KtTokenId.QUEST, "QUEST")
    val COLONCOLON: SyntaxElementType = register(KtTokenId.COLONCOLON, "COLONCOLON")
    val COLON: SyntaxElementType = register(KtTokenId.COLON, "COLON")
    val SEMICOLON: SyntaxElementType = register(KtTokenId.SEMICOLON, "SEMICOLON")
    val DOUBLE_SEMICOLON: SyntaxElementType = register(KtTokenId.DOUBLE_SEMICOLON, "DOUBLE_SEMICOLON")
    val RANGE: SyntaxElementType = register(KtTokenId.RANGE, "RANGE")
    val RANGE_UNTIL: SyntaxElementType = register(KtTokenId.RANGE_UNTIL, "RANGE_UNTIL")
    val EQ: SyntaxElementType = register(KtTokenId.EQ, "EQ")
    val MULTEQ: SyntaxElementType = register(KtTokenId.MULTEQ, "MULTEQ")
    val DIVEQ: SyntaxElementType = register(KtTokenId.DIVEQ, "DIVEQ")
    val PERCEQ: SyntaxElementType = register(KtTokenId.PERCEQ, "PERCEQ")
    val PLUSEQ: SyntaxElementType = register(KtTokenId.PLUSEQ, "PLUSEQ")
    val MINUSEQ: SyntaxElementType = register(KtTokenId.MINUSEQ, "MINUSEQ")

    val NOT_IN: SyntaxElementType = registerToken(KtTokenId.NOT_IN, "NOT_IN", soft = false, modifier = false)
    val NOT_IS: SyntaxElementType = registerToken(KtTokenId.NOT_IS, "NOT_IS", soft = false, modifier = false)

    val HASH: SyntaxElementType = register(KtTokenId.HASH, "HASH")
    val AT: SyntaxElementType = register(KtTokenId.AT, "AT")

    val COMMA: SyntaxElementType = register(KtTokenId.COMMA, "COMMA")

    val EOL_OR_SEMICOLON: SyntaxElementType = register(KtTokenId.EOL_OR_SEMICOLON, "EOL_OR_SEMICOLON")

    val ALL_KEYWORD: SyntaxElementType = registerToken(KtTokenId.ALL_KEYWORD, "all", soft = true, modifier = false)
    val FILE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.FILE_KEYWORD, "file", soft = true, modifier = false)
    val FIELD_KEYWORD: SyntaxElementType = registerToken(KtTokenId.FIELD_KEYWORD, "field", soft = true, modifier = false)
    val PROPERTY_KEYWORD: SyntaxElementType = registerToken(KtTokenId.PROPERTY_KEYWORD, "property", soft = true, modifier = false)
    val RECEIVER_KEYWORD: SyntaxElementType = registerToken(KtTokenId.RECEIVER_KEYWORD, "receiver", soft = true, modifier = false)
    val PARAM_KEYWORD: SyntaxElementType = registerToken(KtTokenId.PARAM_KEYWORD, "param", soft = true, modifier = false)
    val SETPARAM_KEYWORD: SyntaxElementType = registerToken(KtTokenId.SETPARAM_KEYWORD, "setparam", soft = true, modifier = false)
    val DELEGATE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.DELEGATE_KEYWORD, "delegate", soft = true, modifier = false)
    val IMPORT_KEYWORD: SyntaxElementType = registerToken(KtTokenId.IMPORT_KEYWORD, "import", soft = true, modifier = false)
    val WHERE_KEYWORD: SyntaxElementType = registerToken(KtTokenId.WHERE_KEYWORD, "where", soft = true, modifier = false)
    val BY_KEYWORD: SyntaxElementType = registerToken(KtTokenId.BY_KEYWORD, "by", soft = true, modifier = false)
    val GET_KEYWORD: SyntaxElementType = registerToken(KtTokenId.GET_KEYWORD, "get", soft = true, modifier = false)
    val SET_KEYWORD: SyntaxElementType = registerToken(KtTokenId.SET_KEYWORD, "set", soft = true, modifier = false)
    val CONSTRUCTOR_KEYWORD: SyntaxElementType = registerToken(KtTokenId.CONSTRUCTOR_KEYWORD, "constructor", soft = true, modifier = false)
    val INIT_KEYWORD: SyntaxElementType = registerToken(KtTokenId.INIT_KEYWORD, "init", soft = true, modifier = false)
    val CONTEXT_KEYWORD: SyntaxElementType = registerToken(KtTokenId.CONTEXT_KEYWORD, "context", soft = true, modifier = false)

    val ABSTRACT_MODIFIER: SyntaxElementType = registerToken(KtTokenId.ABSTRACT_MODIFIER, "abstract", soft = true, modifier = true)
    val ENUM_MODIFIER: SyntaxElementType = registerToken(KtTokenId.ENUM_MODIFIER, "enum", soft = true, modifier = true)
    val CONTRACT_MODIFIER: SyntaxElementType = registerToken(KtTokenId.CONTRACT_MODIFIER, "contract", soft = true, modifier = true)
    val OPEN_MODIFIER: SyntaxElementType = registerToken(KtTokenId.OPEN_MODIFIER, "open", soft = true, modifier = true)
    val INNER_MODIFIER: SyntaxElementType = registerToken(KtTokenId.INNER_MODIFIER, "inner", soft = true, modifier = true)
    val OVERRIDE_MODIFIER: SyntaxElementType = registerToken(KtTokenId.OVERRIDE_MODIFIER, "override", soft = true, modifier = true)
    val PRIVATE_MODIFIER: SyntaxElementType = registerToken(KtTokenId.PRIVATE_MODIFIER, "private", soft = true, modifier = true)
    val PUBLIC_MODIFIER: SyntaxElementType = registerToken(KtTokenId.PUBLIC_MODIFIER, "public", soft = true, modifier = true)
    val INTERNAL_MODIFIER: SyntaxElementType = registerToken(KtTokenId.INTERNAL_MODIFIER, "internal", soft = true, modifier = true)
    val PROTECTED_MODIFIER: SyntaxElementType = registerToken(KtTokenId.PROTECTED_MODIFIER, "protected", soft = true, modifier = true)

    val CATCH_KEYWORD: SyntaxElementType = registerToken(KtTokenId.CATCH_KEYWORD, "catch", soft = true, modifier = false)

    val OUT_MODIFIER: SyntaxElementType = registerToken(KtTokenId.OUT_MODIFIER, "out", soft = true, modifier = true)
    val VARARG_MODIFIER: SyntaxElementType = registerToken(KtTokenId.VARARG_MODIFIER, "vararg", soft = true, modifier = true)
    val REIFIED_MODIFIER: SyntaxElementType = registerToken(KtTokenId.REIFIED_MODIFIER, "reified", soft = true, modifier = true)

    val DYNAMIC_KEYWORD: SyntaxElementType = registerToken(KtTokenId.DYNAMIC_KEYWORD, "dynamic", soft = true, modifier = false)

    val COMPANION_MODIFIER: SyntaxElementType = registerToken(KtTokenId.COMPANION_MODIFIER, "companion", soft = true, modifier = true)
    val SEALED_MODIFIER: SyntaxElementType = registerToken(KtTokenId.SEALED_MODIFIER, "sealed", soft = true, modifier = true)

    val FINALLY_KEYWORD: SyntaxElementType = registerToken(KtTokenId.FINALLY_KEYWORD, "finally", soft = true, modifier = false)
    val FINAL_MODIFIER: SyntaxElementType = registerToken(KtTokenId.FINAL_MODIFIER, "final", soft = true, modifier = true)

    val LATEINIT_MODIFIER: SyntaxElementType = registerToken(KtTokenId.LATEINIT_MODIFIER, "lateinit", soft = true, modifier = true)

    val DATA_MODIFIER: SyntaxElementType = registerToken(KtTokenId.DATA_MODIFIER, "data", soft = true, modifier = true)
    val VALUE_MODIFIER: SyntaxElementType = registerToken(KtTokenId.VALUE_MODIFIER, "value", soft = true, modifier = true)
    val INLINE_MODIFIER: SyntaxElementType = registerToken(KtTokenId.INLINE_MODIFIER, "inline", soft = true, modifier = true)
    val NOINLINE_MODIFIER: SyntaxElementType = registerToken(KtTokenId.NOINLINE_MODIFIER, "noinline", soft = true, modifier = true)
    val TAILREC_MODIFIER: SyntaxElementType = registerToken(KtTokenId.TAILREC_MODIFIER, "tailrec", soft = true, modifier = true)
    val EXTERNAL_MODIFIER: SyntaxElementType = registerToken(KtTokenId.EXTERNAL_MODIFIER, "external", soft = true, modifier = true)
    val ANNOTATION_MODIFIER: SyntaxElementType = registerToken(KtTokenId.ANNOTATION_MODIFIER, "annotation", soft = true, modifier = true)
    val CROSSINLINE_MODIFIER: SyntaxElementType = registerToken(KtTokenId.CROSSINLINE_MODIFIER, "crossinline", soft = true, modifier = true)
    val OPERATOR_MODIFIER: SyntaxElementType = registerToken(KtTokenId.OPERATOR_MODIFIER, "operator", soft = true, modifier = true)
    val INFIX_MODIFIER: SyntaxElementType = registerToken(KtTokenId.INFIX_MODIFIER, "infix", soft = true, modifier = true)
    val ERROR_MODIFIER: SyntaxElementType = registerToken(KtTokenId.ERROR_MODIFIER, "error", soft = true, modifier = true)

    val CONST_MODIFIER: SyntaxElementType = registerToken(KtTokenId.CONST_MODIFIER, "const", soft = true, modifier = true)

    val SUSPEND_MODIFIER: SyntaxElementType = registerToken(KtTokenId.SUSPEND_MODIFIER, "suspend", soft = true, modifier = true)

    val EXPECT_MODIFIER: SyntaxElementType = registerToken(KtTokenId.EXPECT_MODIFIER, "expect", soft = true, modifier = true)
    val ACTUAL_MODIFIER: SyntaxElementType = registerToken(KtTokenId.ACTUAL_MODIFIER, "actual", soft = true, modifier = true)

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
