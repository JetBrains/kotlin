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
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds

@ApiStatus.Experimental
object KtTokens : SyntaxElementTypesWithIds() {
    private val softKeywordsAndModifiers: MutableSet<SyntaxElementType> = mutableSetOf()
    private val hardKeywordsAndModifiers: MutableSet<SyntaxElementType> = mutableSetOf()
    private val allModifiers: MutableSet<SyntaxElementType> = mutableSetOf()

    val EOF: SyntaxElementType = register(KotlinSyntaxTokenIds.EOF, "EOF")

    val RESERVED: SyntaxElementType = register(KotlinSyntaxTokenIds.RESERVED, "RESERVED")

    val BLOCK_COMMENT: SyntaxElementType = register(KotlinSyntaxTokenIds.BLOCK_COMMENT, "BLOCK_COMMENT")
    val EOL_COMMENT: SyntaxElementType = register(KotlinSyntaxTokenIds.EOL_COMMENT, "EOL_COMMENT")
    val SHEBANG_COMMENT: SyntaxElementType = register(KotlinSyntaxTokenIds.SHEBANG_COMMENT, "SHEBANG_COMMENT")

    val DOC_COMMENT: SyntaxElementType = register(KotlinSyntaxTokenIds.DOC_COMMENT, "KDoc")

    val WHITE_SPACE: SyntaxElementType = SyntaxTokenTypes.WHITE_SPACE

    val INTEGER_LITERAL: SyntaxElementType = register(KotlinSyntaxTokenIds.INTEGER_LITERAL, "INTEGER_LITERAL")
    val FLOAT_LITERAL: SyntaxElementType = register(KotlinSyntaxTokenIds.FLOAT_LITERAL, "FLOAT_CONSTANT")
    val CHARACTER_LITERAL: SyntaxElementType = register(KotlinSyntaxTokenIds.CHARACTER_LITERAL, "CHARACTER_LITERAL")

    val INTERPOLATION_PREFIX: SyntaxElementType = register(KotlinSyntaxTokenIds.INTERPOLATION_PREFIX, "INTERPOLATION_PREFIX")
    val CLOSING_QUOTE: SyntaxElementType = register(KotlinSyntaxTokenIds.CLOSING_QUOTE, "CLOSING_QUOTE")
    val OPEN_QUOTE: SyntaxElementType = register(KotlinSyntaxTokenIds.OPEN_QUOTE, "OPEN_QUOTE")
    val REGULAR_STRING_PART: SyntaxElementType = register(KotlinSyntaxTokenIds.REGULAR_STRING_PART, "REGULAR_STRING_PART")
    val ESCAPE_SEQUENCE: SyntaxElementType = register(KotlinSyntaxTokenIds.ESCAPE_SEQUENCE, "ESCAPE_SEQUENCE")
    val SHORT_TEMPLATE_ENTRY_START: SyntaxElementType = register(KotlinSyntaxTokenIds.SHORT_TEMPLATE_ENTRY_START, "SHORT_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_START: SyntaxElementType = register(KotlinSyntaxTokenIds.LONG_TEMPLATE_ENTRY_START, "LONG_TEMPLATE_ENTRY_START")
    val LONG_TEMPLATE_ENTRY_END: SyntaxElementType = register(KotlinSyntaxTokenIds.LONG_TEMPLATE_ENTRY_END, "LONG_TEMPLATE_ENTRY_END")
    val DANGLING_NEWLINE: SyntaxElementType = register(KotlinSyntaxTokenIds.DANGLING_NEWLINE, "DANGLING_NEWLINE")

    val PACKAGE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.PACKAGE_KEYWORD, "package", soft = false, modifier = false)
    val AS_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.AS_KEYWORD, "as", soft = false, modifier = false)
    val TYPE_ALIAS_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.TYPE_ALIAS_KEYWORD, "typealias", soft = false, modifier = false)
    val CLASS_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.CLASS_KEYWORD, "class", soft = false, modifier = false)
    val THIS_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.THIS_KEYWORD, "this", soft = false, modifier = false)
    val SUPER_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.SUPER_KEYWORD, "super", soft = false, modifier = false)
    val VAL_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.VAL_KEYWORD, "val", soft = false, modifier = false)
    val VAR_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.VAR_KEYWORD, "var", soft = false, modifier = false)

    val FUN_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.FUN_MODIFIER, "fun", soft = false, modifier = true)

    val FOR_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.FOR_KEYWORD, "for", soft = false, modifier = false)
    val NULL_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.NULL_KEYWORD, "null", soft = false, modifier = false)
    val TRUE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.TRUE_KEYWORD, "true", soft = false, modifier = false)
    val FALSE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.FALSE_KEYWORD, "false", soft = false, modifier = false)
    val IS_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.IS_KEYWORD, "is", soft = false, modifier = false)

    val IN_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.IN_MODIFIER, "in", soft = false, modifier = true)

    val THROW_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.THROW_KEYWORD, "throw", soft = false, modifier = false)
    val RETURN_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.RETURN_KEYWORD, "return", soft = false, modifier = false)
    val BREAK_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.BREAK_KEYWORD, "break", soft = false, modifier = false)
    val CONTINUE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.CONTINUE_KEYWORD, "continue", soft = false, modifier = false)
    val OBJECT_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.OBJECT_KEYWORD, "object", soft = false, modifier = false)
    val IF_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.IF_KEYWORD, "if", soft = false, modifier = false)
    val TRY_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.TRY_KEYWORD, "try", soft = false, modifier = false)
    val ELSE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.ELSE_KEYWORD, "else", soft = false, modifier = false)
    val WHILE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.WHILE_KEYWORD, "while", soft = false, modifier = false)
    val DO_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.DO_KEYWORD, "do", soft = false, modifier = false)
    val WHEN_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.WHEN_KEYWORD, "when", soft = false, modifier = false)
    val INTERFACE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.INTERFACE_KEYWORD, "interface", soft = false, modifier = false)

    // Reserved for future use:
    val TYPEOF_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.TYPEOF_KEYWORD, "typeof", soft = false, modifier = false)

    val AS_SAFE: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.AS_SAFE, "AS_SAFE", soft = false, modifier = false)

    val IDENTIFIER: SyntaxElementType = register(KotlinSyntaxTokenIds.IDENTIFIER, "IDENTIFIER")

    val FIELD_IDENTIFIER: SyntaxElementType = register(KotlinSyntaxTokenIds.FIELD_IDENTIFIER, "FIELD_IDENTIFIER")
    val LBRACKET: SyntaxElementType = register(KotlinSyntaxTokenIds.LBRACKET, "LBRACKET")
    val RBRACKET: SyntaxElementType = register(KotlinSyntaxTokenIds.RBRACKET, "RBRACKET")
    val LBRACE: SyntaxElementType = register(KotlinSyntaxTokenIds.LBRACE, "LBRACE")
    val RBRACE: SyntaxElementType = register(KotlinSyntaxTokenIds.RBRACE, "RBRACE")
    val LPAR: SyntaxElementType = register(KotlinSyntaxTokenIds.LPAR, "LPAR")
    val RPAR: SyntaxElementType = register(KotlinSyntaxTokenIds.RPAR, "RPAR")
    val DOT: SyntaxElementType = register(KotlinSyntaxTokenIds.DOT, "DOT")
    val PLUSPLUS: SyntaxElementType = register(KotlinSyntaxTokenIds.PLUSPLUS, "PLUSPLUS")
    val MINUSMINUS: SyntaxElementType = register(KotlinSyntaxTokenIds.MINUSMINUS, "MINUSMINUS")
    val MUL: SyntaxElementType = register(KotlinSyntaxTokenIds.MUL, "MUL")
    val PLUS: SyntaxElementType = register(KotlinSyntaxTokenIds.PLUS, "PLUS")
    val MINUS: SyntaxElementType = register(KotlinSyntaxTokenIds.MINUS, "MINUS")
    val EXCL: SyntaxElementType = register(KotlinSyntaxTokenIds.EXCL, "EXCL")
    val DIV: SyntaxElementType = register(KotlinSyntaxTokenIds.DIV, "DIV")
    val PERC: SyntaxElementType = register(KotlinSyntaxTokenIds.PERC, "PERC")
    val LT: SyntaxElementType = register(KotlinSyntaxTokenIds.LT, "LT")
    val GT: SyntaxElementType = register(KotlinSyntaxTokenIds.GT, "GT")
    val LTEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.LTEQ, "LTEQ")
    val GTEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.GTEQ, "GTEQ")
    val EQEQEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.EQEQEQ, "EQEQEQ")
    val ARROW: SyntaxElementType = register(KotlinSyntaxTokenIds.ARROW, "ARROW")
    val DOUBLE_ARROW: SyntaxElementType = register(KotlinSyntaxTokenIds.DOUBLE_ARROW, "DOUBLE_ARROW")
    val EXCLEQEQEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.EXCLEQEQEQ, "EXCLEQEQEQ")
    val EQEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.EQEQ, "EQEQ")
    val EXCLEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.EXCLEQ, "EXCLEQ")
    val EXCLEXCL: SyntaxElementType = register(KotlinSyntaxTokenIds.EXCLEXCL, "EXCLEXCL")
    val ANDAND: SyntaxElementType = register(KotlinSyntaxTokenIds.ANDAND, "ANDAND")
    val AND: SyntaxElementType = register(KotlinSyntaxTokenIds.AND, "AND")
    val OROR: SyntaxElementType = register(KotlinSyntaxTokenIds.OROR, "OROR")
    val OR: SyntaxElementType = register(KotlinSyntaxTokenIds.OR, "OR")
    val SAFE_ACCESS: SyntaxElementType = register(KotlinSyntaxTokenIds.SAFE_ACCESS, "SAFE_ACCESS")
    val ERROR_SAFE_ACCESS: SyntaxElementType = register(KotlinSyntaxTokenIds.ERROR_SAFE_ACCESS, "ERROR_SAFE_ACCESS")
    val ELVIS: SyntaxElementType = register(KotlinSyntaxTokenIds.ELVIS, "ELVIS")
    val QUEST: SyntaxElementType = register(KotlinSyntaxTokenIds.QUEST, "QUEST")
    val COLONCOLON: SyntaxElementType = register(KotlinSyntaxTokenIds.COLONCOLON, "COLONCOLON")
    val COLON: SyntaxElementType = register(KotlinSyntaxTokenIds.COLON, "COLON")
    val SEMICOLON: SyntaxElementType = register(KotlinSyntaxTokenIds.SEMICOLON, "SEMICOLON")
    val DOUBLE_SEMICOLON: SyntaxElementType = register(KotlinSyntaxTokenIds.DOUBLE_SEMICOLON, "DOUBLE_SEMICOLON")
    val RANGE: SyntaxElementType = register(KotlinSyntaxTokenIds.RANGE, "RANGE")
    val RANGE_UNTIL: SyntaxElementType = register(KotlinSyntaxTokenIds.RANGE_UNTIL, "RANGE_UNTIL")
    val EQ: SyntaxElementType = register(KotlinSyntaxTokenIds.EQ, "EQ")
    val MULTEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.MULTEQ, "MULTEQ")
    val DIVEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.DIVEQ, "DIVEQ")
    val PERCEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.PERCEQ, "PERCEQ")
    val PLUSEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.PLUSEQ, "PLUSEQ")
    val MINUSEQ: SyntaxElementType = register(KotlinSyntaxTokenIds.MINUSEQ, "MINUSEQ")

    val NOT_IN: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.NOT_IN, "NOT_IN", soft = false, modifier = false)
    val NOT_IS: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.NOT_IS, "NOT_IS", soft = false, modifier = false)

    val HASH: SyntaxElementType = register(KotlinSyntaxTokenIds.HASH, "HASH")
    val AT: SyntaxElementType = register(KotlinSyntaxTokenIds.AT, "AT")

    val COMMA: SyntaxElementType = register(KotlinSyntaxTokenIds.COMMA, "COMMA")

    val EOL_OR_SEMICOLON: SyntaxElementType = register(KotlinSyntaxTokenIds.EOL_OR_SEMICOLON, "EOL_OR_SEMICOLON")

    val ALL_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.ALL_KEYWORD, "all", soft = true, modifier = false)
    val FILE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.FILE_KEYWORD, "file", soft = true, modifier = false)
    val FIELD_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.FIELD_KEYWORD, "field", soft = true, modifier = false)
    val PROPERTY_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.PROPERTY_KEYWORD, "property", soft = true, modifier = false)
    val RECEIVER_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.RECEIVER_KEYWORD, "receiver", soft = true, modifier = false)
    val PARAM_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.PARAM_KEYWORD, "param", soft = true, modifier = false)
    val SETPARAM_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.SETPARAM_KEYWORD, "setparam", soft = true, modifier = false)
    val DELEGATE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.DELEGATE_KEYWORD, "delegate", soft = true, modifier = false)
    val IMPORT_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.IMPORT_KEYWORD, "import", soft = true, modifier = false)
    val WHERE_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.WHERE_KEYWORD, "where", soft = true, modifier = false)
    val BY_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.BY_KEYWORD, "by", soft = true, modifier = false)
    val GET_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.GET_KEYWORD, "get", soft = true, modifier = false)
    val SET_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.SET_KEYWORD, "set", soft = true, modifier = false)
    val CONSTRUCTOR_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.CONSTRUCTOR_KEYWORD, "constructor", soft = true, modifier = false)
    val INIT_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.INIT_KEYWORD, "init", soft = true, modifier = false)
    val CONTEXT_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.CONTEXT_KEYWORD, "context", soft = true, modifier = false)

    val ABSTRACT_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.ABSTRACT_MODIFIER, "abstract", soft = true, modifier = true)
    val ENUM_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.ENUM_MODIFIER, "enum", soft = true, modifier = true)
    val CONTRACT_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.CONTRACT_MODIFIER, "contract", soft = true, modifier = true)
    val OPEN_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.OPEN_MODIFIER, "open", soft = true, modifier = true)
    val INNER_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.INNER_MODIFIER, "inner", soft = true, modifier = true)
    val OVERRIDE_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.OVERRIDE_MODIFIER, "override", soft = true, modifier = true)
    val PRIVATE_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.PRIVATE_MODIFIER, "private", soft = true, modifier = true)
    val PUBLIC_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.PUBLIC_MODIFIER, "public", soft = true, modifier = true)
    val INTERNAL_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.INTERNAL_MODIFIER, "internal", soft = true, modifier = true)
    val PROTECTED_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.PROTECTED_MODIFIER, "protected", soft = true, modifier = true)

    val CATCH_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.CATCH_KEYWORD, "catch", soft = true, modifier = false)

    val OUT_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.OUT_MODIFIER, "out", soft = true, modifier = true)
    val VARARG_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.VARARG_MODIFIER, "vararg", soft = true, modifier = true)
    val REIFIED_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.REIFIED_MODIFIER, "reified", soft = true, modifier = true)

    val DYNAMIC_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.DYNAMIC_KEYWORD, "dynamic", soft = true, modifier = false)

    val COMPANION_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.COMPANION_MODIFIER, "companion", soft = true, modifier = true)
    val SEALED_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.SEALED_MODIFIER, "sealed", soft = true, modifier = true)

    val FINALLY_KEYWORD: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.FINALLY_KEYWORD, "finally", soft = true, modifier = false)
    val FINAL_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.FINAL_MODIFIER, "final", soft = true, modifier = true)

    val LATEINIT_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.LATEINIT_MODIFIER, "lateinit", soft = true, modifier = true)

    val DATA_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.DATA_MODIFIER, "data", soft = true, modifier = true)
    val VALUE_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.VALUE_MODIFIER, "value", soft = true, modifier = true)
    val INLINE_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.INLINE_MODIFIER, "inline", soft = true, modifier = true)
    val NOINLINE_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.NOINLINE_MODIFIER, "noinline", soft = true, modifier = true)
    val TAILREC_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.TAILREC_MODIFIER, "tailrec", soft = true, modifier = true)
    val EXTERNAL_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.EXTERNAL_MODIFIER, "external", soft = true, modifier = true)
    val ANNOTATION_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.ANNOTATION_MODIFIER, "annotation", soft = true, modifier = true)
    val CROSSINLINE_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.CROSSINLINE_MODIFIER, "crossinline", soft = true, modifier = true)
    val OPERATOR_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.OPERATOR_MODIFIER, "operator", soft = true, modifier = true)
    val INFIX_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.INFIX_MODIFIER, "infix", soft = true, modifier = true)
    val ERROR_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.ERROR_MODIFIER, "error", soft = true, modifier = true)

    val CONST_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.CONST_MODIFIER, "const", soft = true, modifier = true)

    val SUSPEND_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.SUSPEND_MODIFIER, "suspend", soft = true, modifier = true)

    val EXPECT_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.EXPECT_MODIFIER, "expect", soft = true, modifier = true)
    val ACTUAL_MODIFIER: SyntaxElementType = registerToken(KotlinSyntaxTokenIds.ACTUAL_MODIFIER, "actual", soft = true, modifier = true)

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
