/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.contracts

import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.expressions.FirConstKind
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.util.OperatorNameConventions

private object ConeConditionalEffectToFirVisitor : ConeContractDescriptionVisitor<FirExpression?, Map<Int, FirExpression>>() {
    override fun visitConditionalEffectDeclaration(conditionalEffect: ConeConditionalEffectDeclaration, data: Map<Int, FirExpression>): FirExpression? {
        return conditionalEffect.condition.accept(this, data)
    }

    override fun visitConstantDescriptor(constantReference: ConeConstantReference, data: Map<Int, FirExpression>): FirExpression? {
        return when (constantReference) {
            ConeBooleanConstantReference.TRUE -> buildConstExpression(null, FirConstKind.Boolean, true)
            ConeBooleanConstantReference.FALSE -> buildConstExpression(null, FirConstKind.Boolean, false)
            ConeConstantReference.NULL -> createConstNull()
            else -> null
        }
    }

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: ConeBinaryLogicExpression,
        data: Map<Int, FirExpression>
    ): FirExpression? {
        val leftExpression = binaryLogicExpression.left.accept(this, data) ?: return null
        val rightExpression = binaryLogicExpression.right.accept(this, data) ?: return null
        return buildBinaryLogicExpression {
            leftOperand = leftExpression
            rightOperand = rightExpression
            binaryLogicExpression.kind
        }
    }

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Map<Int, FirExpression>): FirExpression? {
        val explicitReceiver = logicalNot.arg.accept(this, data) ?: return null
        return buildFunctionCall {
            calleeReference = buildSimpleNamedReference { name = OperatorNameConventions.NOT }
            this.explicitReceiver = explicitReceiver
        }
    }

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Map<Int, FirExpression>): FirExpression? {
        val argument = isInstancePredicate.arg.accept(this@ConeConditionalEffectToFirVisitor, data) ?: return null
        return buildTypeOperatorCall {
            argumentList = buildUnaryArgumentList(argument)
            operation = if (isInstancePredicate.isNegated) {
                FirOperation.NOT_IS
            } else {
                FirOperation.IS
            }
            conversionTypeRef = buildResolvedTypeRef { type = isInstancePredicate.type }
        }
    }

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Map<Int, FirExpression>): FirExpression? {
        val argument = isNullPredicate.arg.accept(this, data) ?: return null
        return buildOperatorCall {
            operation = if (isNullPredicate.isNegated) {
                FirOperation.NOT_EQ
            } else {
                FirOperation.EQ
            }
            argumentList = buildBinaryArgumentList(argument, createConstNull())
        }
    }

    override fun visitValueParameterReference(valueParameterReference: ConeValueParameterReference, data: Map<Int, FirExpression>): FirExpression? {
        return data[valueParameterReference.parameterIndex]
    }

    private fun createConstNull(): FirConstExpression<*> = buildConstExpression(null, FirConstKind.Null, null)
}

fun ConeConditionalEffectDeclaration.buildContractFir(argumentMapping: Map<Int, FirExpression>): FirExpression? {
    return condition.accept(ConeConditionalEffectToFirVisitor, argumentMapping)
}

fun createArgumentsMapping(functionCall: FirFunctionCall): Map<Int, FirExpression>? {
    val function = functionCall.toResolvedCallableSymbol()?.fir as? FirSimpleFunction ?: return null
    val argumentsMapping = mutableMapOf<Int, FirExpression>()
    // TODO: process implicit receiver
    // TODO: change to mapping from candidate
    //  problem: we have already resolved reference without candidate
    functionCall.explicitReceiver?.let { argumentsMapping[-1] = it }


    val nameToIndex = function.valueParameters.mapIndexed { index, parameter -> parameter.name to index }.toMap()
    functionCall.arguments.forEachIndexed { index, argument ->
        when (argument) {
            is FirNamedArgumentExpression -> argumentsMapping[nameToIndex[argument.name]!!] = argument
            else -> argumentsMapping[index] = argument
        }
    }
    return argumentsMapping
}