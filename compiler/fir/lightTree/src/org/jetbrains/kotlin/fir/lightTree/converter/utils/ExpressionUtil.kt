/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter.utils

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.getAsString
import org.jetbrains.kotlin.fir.lightTree.converter.ConverterUtil.nameAsSafeName
import org.jetbrains.kotlin.fir.lightTree.converter.ExpressionsConverter
import org.jetbrains.kotlin.fir.references.FirSimpleNamedReference
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.DOT
import org.jetbrains.kotlin.lexer.KtTokens.SAFE_ACCESS
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parsing.KotlinExpressionParsing
import org.jetbrains.kotlin.util.OperatorNameConventions

val qualifiedAccessTokens = TokenSet.create(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION)

fun String.getOperationSymbol(): IElementType {
    KotlinExpressionParsing.ALL_OPERATIONS.types.forEach {
        if (it is KtSingleValueToken && it.value == this) return it
    }
    return KtTokens.IDENTIFIER
}

fun ExpressionsConverter.convertAssignment(
    leftArgNode: LighterASTNode,
    rightArgAsFir: FirExpression,
    operation: FirOperation
): FirStatement {
    if (leftArgNode.tokenType == PARENTHESIZED) {
        return convertAssignment(leftArgNode.getExpressionInParentheses(), rightArgAsFir, operation)
    }
    if (leftArgNode.tokenType == ARRAY_ACCESS_EXPRESSION) {
        val arrayAccessFunctionCall = getAsFirExpression(leftArgNode) as FirFunctionCall
        val firArrayExpression = arrayAccessFunctionCall.explicitReceiver!!
        val arraySet = if (operation != FirOperation.ASSIGN) {
            FirArraySetCallImpl(session, null, rightArgAsFir, operation).apply {
                indexes += arrayAccessFunctionCall.arguments
            }
        } else {
            return FirFunctionCallImpl(session, null).apply {
                calleeReference = FirSimpleNamedReference(this@convertAssignment.session, null, OperatorNameConventions.SET)
                explicitReceiver = firArrayExpression
                arguments += arrayAccessFunctionCall.arguments
                arguments += rightArgAsFir
            }
        }
        if (leftArgNode.getChildNodesByType(REFERENCE_EXPRESSION).isNotEmpty()) {
            return arraySet.apply {
                lValue = (firArrayExpression as FirQualifiedAccess).calleeReference
            }
        }
        return FirBlockImpl(this@convertAssignment.session, null).apply {
            val name = Name.special("<array-set>")
            statements += generateTemporaryVariable(this@convertAssignment.session, null, name, firArrayExpression)
            statements += arraySet.apply { lValue = FirSimpleNamedReference(this@convertAssignment.session, null, name) }
        }
    }
    if (operation != FirOperation.ASSIGN &&
        leftArgNode.tokenType != REFERENCE_EXPRESSION && leftArgNode.tokenType != THIS_EXPRESSION &&
        (leftArgNode.tokenType !in qualifiedAccessTokens || getSelectorType(leftArgNode.getChildrenAsArray()) != REFERENCE_EXPRESSION)
    ) {
        return FirBlockImpl(session, null).apply {
            val name = Name.special("<complex-set>")
            statements += generateTemporaryVariable(this@convertAssignment.session, null, name, getAsFirExpression(leftArgNode))
            statements += FirVariableAssignmentImpl(this@convertAssignment.session, null, rightArgAsFir, operation).apply {
                lValue = FirSimpleNamedReference(this@convertAssignment.session, null, name)
            }
        }
    }
    return FirVariableAssignmentImpl(session, null, rightArgAsFir, operation).apply {
        lValue = convertLValue(leftArgNode, this)
    }

}

fun ExpressionsConverter.generateIncrementOrDecrementBlock(
    argument: LighterASTNode?,
    callName: Name,
    prefix: Boolean
): FirExpression {
    if (argument == null) {
        return FirErrorExpressionImpl(session, null, "Inc/dec without operand")
    }
    return FirBlockImpl(session, null).apply {
        val tempName = Name.special("<unary>")
        val temporaryVariable = generateTemporaryVariable(
            this@generateIncrementOrDecrementBlock.session, null, tempName, getAsFirExpression(argument)
        )
        statements += temporaryVariable
        val resultName = Name.special("<unary-result>")
        val resultInitializer = FirFunctionCallImpl(this@generateIncrementOrDecrementBlock.session, null).apply {
            this.calleeReference = FirSimpleNamedReference(this@generateIncrementOrDecrementBlock.session, null, callName)
            this.explicitReceiver = generateResolvedAccessExpression(
                this@generateIncrementOrDecrementBlock.session, null, temporaryVariable
            )
        }
        val resultVar = generateTemporaryVariable(this@generateIncrementOrDecrementBlock.session, null, resultName, resultInitializer)
        val assignment = convertAssignment(
            argument,
            if (prefix && argument.tokenType != REFERENCE_EXPRESSION)
                generateResolvedAccessExpression(this@generateIncrementOrDecrementBlock.session, null, resultVar)
            else
                resultInitializer,
            FirOperation.ASSIGN
        )

        fun appendAssignment() {
            if (assignment is FirBlock) {
                statements += assignment.statements
            } else {
                statements += assignment
            }
        }

        if (prefix) {
            if (argument.tokenType != REFERENCE_EXPRESSION) {
                statements += resultVar
                appendAssignment()
                statements += generateResolvedAccessExpression(this@generateIncrementOrDecrementBlock.session, null, resultVar)
            } else {
                appendAssignment()
                statements += generateAccessExpression(
                    this@generateIncrementOrDecrementBlock.session, null, argument.getAsString().nameAsSafeName()
                )
            }
        } else {
            appendAssignment()
            statements += generateResolvedAccessExpression(this@generateIncrementOrDecrementBlock.session, null, temporaryVariable)
        }
    }
}

fun bangBangToWhen(session: FirSession, baseExpression: FirExpression): FirWhenExpression {
    return baseExpression.generateNotNullOrOther(
        session,
        FirThrowExpressionImpl(
            session, null, FirFunctionCallImpl(session, null).apply {
                calleeReference = FirSimpleNamedReference(session, null, RawFirBuilder.KNPE)
            }
        ), "bangbang", null
    )
}

private fun getSelectorType(qualifiedAccessChildren: Array<LighterASTNode?>): IElementType? {
    var isSelector = false
    qualifiedAccessChildren.forEach {
        if (it == null) return null
        when (it.tokenType) {
            DOT, SAFE_ACCESS -> isSelector = true
            else -> if (isSelector) return it.tokenType
        }
    }
    return null
}
