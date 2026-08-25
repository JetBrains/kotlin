/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IErrorCounterReparseableElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType
import org.jetbrains.kotlin.util.getSingleChildOrNull

object ElementTypeUtils {
    @JvmStatic
    fun getKotlinBlockImbalanceCount(seq: CharSequence): Int {
        val lexer = KotlinLexer()

        lexer.start(seq)
        if (lexer.tokenType !== KtTokens.LBRACE) return IErrorCounterReparseableElementType.FATAL_ERROR
        lexer.advance()
        var balance = 1
        while (lexer.tokenType != KtTokens.EOF) {
            val type = lexer.tokenType ?: break
            if (balance == 0) {
                return IErrorCounterReparseableElementType.FATAL_ERROR
            }
            if (type === KtTokens.LBRACE) {
                balance++
            } else if (type === KtTokens.RBRACE) {
                balance--
            }
            lexer.advance()
        }
        return balance
    }

    fun LighterASTNode.getOperationSymbol(tree: FlyweightCapableTreeStructure<LighterASTNode>): KtToken {
        assert(tokenType == OPERATION_REFERENCE)
        // Actually, all types are `KtSingleValueToken` except `IDENTIFIER` that is `KtToken`. The latter is used for infix functions.
        return getSingleChildOrNull(tree)!!.tokenType as KtToken
    }

    /**
     * Whether the node represents [org.jetbrains.kotlin.psi.KtExpression].
     *
     * Must be updated once a new non-expression element is added to the parser.
     */
    fun LighterASTNode.isExpression(): Boolean = when (tokenType) {
        // Stub-based element types that are not `KtExpression`
        CLASS_BODY,
        COMPANION_BLOCK,
        INITIALIZER_LIST,
        VALUE_PARAMETER_LIST,
        CONTEXT_PARAMETER_LIST,
        CONTEXT_RECEIVER,
        TYPE_PARAMETER_LIST,
        TYPE_CONSTRAINT_LIST,
        TYPE_CONSTRAINT,
        SUPER_TYPE_LIST,
        DELEGATED_SUPER_TYPE_ENTRY,
        SUPER_TYPE_CALL_ENTRY,
        SUPER_TYPE_ENTRY,
        MODIFIER_LIST,
        ANNOTATION,
        ANNOTATION_ENTRY,
        ANNOTATION_TARGET,
        TYPE_REFERENCE,
        USER_TYPE,
        DYNAMIC_TYPE,
        FUNCTION_TYPE,
        FUNCTION_TYPE_RECEIVER,
        NULLABLE_TYPE,
        INTERSECTION_TYPE,
        TYPE_PROJECTION,
        LONG_STRING_TEMPLATE_ENTRY,
        SHORT_STRING_TEMPLATE_ENTRY,
        LITERAL_STRING_TEMPLATE_ENTRY,
        ESCAPE_STRING_TEMPLATE_ENTRY,
        STRING_INTERPOLATION_PREFIX,
        TYPE_ARGUMENT_LIST,
        VALUE_ARGUMENT_LIST,
        VALUE_ARGUMENT,
        CONTRACT_EFFECT_LIST,
        CONTRACT_EFFECT,
        LAMBDA_ARGUMENT,
        VALUE_ARGUMENT_NAME,
        PACKAGE_DIRECTIVE,
        FILE_ANNOTATION_LIST,
        IMPORT_LIST,
        IMPORT_DIRECTIVE,
        IMPORT_ALIAS,
            -> false

        is KtNodeType,
        LAMBDA_EXPRESSION,
            -> true

        // All stub-based element types that are not `KtExpression` are listed above
        is KtStubElementType<*, *> -> true

        else -> false
    }
}
