/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.contracts

import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.*
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.util.OperatorNameConventions

private object ConeConditionalEffectToFirVisitor : ConeContractDescriptionVisitor<FirExpression?, Map<Int, FirExpression>>() {
    override fun visitConditionalEffectDeclaration(conditionalEffect: ConeConditionalEffectDeclaration, data: Map<Int, FirExpression>): FirExpression? {
        return conditionalEffect.condition.accept(this, data)
    }

    override fun visitConstantDescriptor(constantReference: ConeConstantReference, data: Map<Int, FirExpression>): FirExpression? {
        return when (constantReference) {
            ConeBooleanConstantReference.TRUE -> FirConstExpressionImpl(null, IrConstKind.Boolean, true)
            ConeBooleanConstantReference.FALSE -> FirConstExpressionImpl(null, IrConstKind.Boolean, false)
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
        return FirBinaryLogicExpressionImpl(null, leftExpression, rightExpression, binaryLogicExpression.kind)
    }

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Map<Int, FirExpression>): FirExpression? {
        val explicitReceiver = logicalNot.arg.accept(this, data) ?: return null
        return FirFunctionCallImpl(null).apply {
            calleeReference = FirSimpleNamedReference(null, OperatorNameConventions.NOT, null)
            this.explicitReceiver = explicitReceiver
        }
    }

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Map<Int, FirExpression>): FirExpression? {
        val operation = if (isInstancePredicate.isNegated) {
            FirOperation.NOT_IS
        } else {
            FirOperation.IS
        }
        return FirTypeOperatorCallImpl(null, operation, FirResolvedTypeRefImpl(null, isInstancePredicate.type))
    }

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Map<Int, FirExpression>): FirExpression? {
        val argument = isNullPredicate.arg.accept(this, data) ?: return null
        val operation = if (isNullPredicate.isNegated) {
            FirOperation.NOT_EQ
        } else {
            FirOperation.EQ
        }
        return FirOperatorCallImpl(null, operation).apply {
            arguments += argument
            arguments += createConstNull()
        }
    }

    override fun visitValueParameterReference(valueParameterReference: ConeValueParameterReference, data: Map<Int, FirExpression>): FirExpression? {
        return data[valueParameterReference.parameterIndex]
    }

    private fun createConstNull(): FirConstExpression<*> = FirConstExpressionImpl(null, IrConstKind.Null, null)
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