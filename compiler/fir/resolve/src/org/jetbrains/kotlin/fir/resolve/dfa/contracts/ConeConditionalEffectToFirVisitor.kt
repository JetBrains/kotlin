/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.contracts

import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.util.OperatorNameConventions

private class ConeConditionalEffectToFirVisitor(
    val valueParametersMapping: Map<Int, FirExpression>,
    val substitutor: ConeSubstitutor
) : ConeContractDescriptionVisitor<FirExpression?, Nothing?>() {
    override fun visitConditionalEffectDeclaration(conditionalEffect: ConeConditionalEffectDeclaration, data: Nothing?): FirExpression? {
        return conditionalEffect.condition.accept(this, data)
    }

    override fun visitConstantDescriptor(constantReference: ConeConstantReference, data: Nothing?): FirExpression? {
        return when (constantReference) {
            ConeBooleanConstantReference.TRUE -> buildConstExpression(null, ConstantValueKind.Boolean, true)
            ConeBooleanConstantReference.FALSE -> buildConstExpression(null, ConstantValueKind.Boolean, false)
            ConeConstantReference.NULL -> createConstNull()
            else -> null
        }
    }

    override fun visitLogicalBinaryOperationContractExpression(
        binaryLogicExpression: ConeBinaryLogicExpression,
        data: Nothing?
    ): FirExpression? {
        val leftExpression = binaryLogicExpression.left.accept(this, data) ?: return null
        val rightExpression = binaryLogicExpression.right.accept(this, data) ?: return null
        return buildBinaryLogicExpression {
            leftOperand = leftExpression
            rightOperand = rightExpression
            kind = binaryLogicExpression.kind
        }
    }

    override fun visitLogicalNot(logicalNot: ConeLogicalNot, data: Nothing?): FirExpression? {
        val explicitReceiver = logicalNot.arg.accept(this, data) ?: return null
        return buildFunctionCall {
            calleeReference = buildSimpleNamedReference { name = OperatorNameConventions.NOT }
            this.explicitReceiver = explicitReceiver
            origin = FirFunctionCallOrigin.Operator
        }
    }

    override fun visitIsInstancePredicate(isInstancePredicate: ConeIsInstancePredicate, data: Nothing?): FirExpression? {
        val argument = isInstancePredicate.arg.accept(this@ConeConditionalEffectToFirVisitor, data) ?: return null
        return buildTypeOperatorCall {
            argumentList = buildUnaryArgumentList(argument)
            operation = if (isInstancePredicate.isNegated) {
                FirOperation.NOT_IS
            } else {
                FirOperation.IS
            }
            conversionTypeRef = buildResolvedTypeRef { type = substitutor.substituteOrSelf(isInstancePredicate.type) }
        }
    }

    override fun visitIsNullPredicate(isNullPredicate: ConeIsNullPredicate, data: Nothing?): FirExpression? {
        val argument = isNullPredicate.arg.accept(this, data) ?: return null
        return buildEqualityOperatorCall {
            operation = if (isNullPredicate.isNegated) {
                FirOperation.NOT_EQ
            } else {
                FirOperation.EQ
            }
            argumentList = buildBinaryArgumentList(argument, createConstNull())
        }
    }

    override fun visitValueParameterReference(valueParameterReference: ConeValueParameterReference, data: Nothing?): FirExpression? {
        return valueParametersMapping[valueParameterReference.parameterIndex]
    }

    private fun createConstNull(): FirConstExpression<*> = buildConstExpression(null, ConstantValueKind.Null, null)
}

fun ConeConditionalEffectDeclaration.buildContractFir(
    argumentMapping: Map<Int, FirExpression>,
    substitutor: ConeSubstitutor
): FirExpression? {
    return condition.accept(ConeConditionalEffectToFirVisitor(argumentMapping, substitutor), null)
}

fun createArgumentsMapping(qualifiedAccess: FirQualifiedAccess): Map<Int, FirExpression>? {
    val argumentsMapping = mutableMapOf<Int, FirExpression>()
    qualifiedAccess.extensionReceiver.takeIf { it != FirNoReceiverExpression }?.let { argumentsMapping[-1] = it }
        ?: qualifiedAccess.dispatchReceiver.takeIf { it != FirNoReceiverExpression }?.let { argumentsMapping[-1] = it }
    when (qualifiedAccess) {
        is FirFunctionCall -> {
            val function = qualifiedAccess.toResolvedCallableSymbol()?.fir as? FirSimpleFunction ?: return null
            val parameterToIndex = function.valueParameters.mapIndexed { index, parameter -> parameter to index }.toMap()
            val callArgumentMapping = qualifiedAccess.argumentMapping ?: return null
            for (argument in qualifiedAccess.arguments) {
                argumentsMapping[parameterToIndex.getValue(callArgumentMapping.getValue(argument))] = argument.unwrapArgument()
            }
        }
        is FirVariableAssignment -> {
            argumentsMapping[0] = qualifiedAccess.rValue
        }
    }
    return argumentsMapping
}

