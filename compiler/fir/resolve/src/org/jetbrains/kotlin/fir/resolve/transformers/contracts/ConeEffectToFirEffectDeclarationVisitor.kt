/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.contracts

import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.builder.buildEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.toFirEffectDeclaration
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.references.builder.buildSimpleNamedReference
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.util.OperatorNameConventions

class ConeEffectToFirEffectDeclarationVisitor(
    private val effectExtractor: ConeEffectExtractor
) : ConeContractDescriptionVisitor<FirExpression?, Map<Int, FirExpression>>() {
    override fun visitConditionalEffectDeclaration(
        conditionalEffect: ConeConditionalEffectDeclaration,
        data: Map<Int, FirExpression>
    ): FirExpression? {
        val effectDeclaration = conditionalEffect.effect.accept(this, data) as? FirEffectDeclaration
        val condition = conditionalEffect.condition
            .accept(this, data)
            ?.accept(effectExtractor, null) as? ConeBooleanExpression
        return if (effectDeclaration != null && condition != null) {
            buildEffectDeclaration {
                this.effect = ConeConditionalEffectDeclaration(effectDeclaration.effect, condition)
            }
        } else {
            null
        }
    }

    override fun visitReturnsEffectDeclaration(
        returnsEffect: ConeReturnsEffectDeclaration,
        data: Map<Int, FirExpression>
    ): FirExpression? {
        return returnsEffect.toFirEffectDeclaration()
    }

    override fun visitCallsEffectDeclaration(callsEffect: ConeCallsEffectDeclaration, data: Map<Int, FirExpression>): FirExpression? {
        val parameter = callsEffect.valueParameterReference
            .accept(this, data)
            ?.accept(effectExtractor, null) as? ConeValueParameterReference
        return if (parameter != null) {
            return buildEffectDeclaration {
                effect = ConeCallsEffectDeclaration(parameter, callsEffect.kind)
            }
        } else {
            null
        }
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
            kind = binaryLogicExpression.kind
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
        val argument = isInstancePredicate.arg.accept(this@ConeEffectToFirEffectDeclarationVisitor, data) ?: return null
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
        return buildEqualityOperatorCall {
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

fun ConeEffectDeclaration.buildContractEffectFir(effectExtractor: ConeEffectExtractor, argumentMapping: Map<Int, FirExpression>): FirExpression? {
    return this.accept(ConeEffectToFirEffectDeclarationVisitor(effectExtractor), argumentMapping)
}
