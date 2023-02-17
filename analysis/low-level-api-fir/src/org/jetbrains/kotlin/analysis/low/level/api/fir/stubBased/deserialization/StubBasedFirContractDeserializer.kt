/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.contracts.description.EffectType
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.ExpressionType
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.builder.buildResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference.Companion.FALSE
import org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference.Companion.TRUE
import org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference.Companion.NOT_NULL
import org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference.Companion.WILDCARD
import org.jetbrains.kotlin.fir.contracts.toFirEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.LogicOperationKind
import org.jetbrains.kotlin.fir.types.isBoolean
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.KtContractEffectList
import org.jetbrains.kotlin.psi.stubs.KotlinContractExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class StubBasedFirContractDeserializer(
    private val simpleFunction: FirSimpleFunction,
    private val typeDeserializer: StubBasedFirTypeDeserializer
) {
    fun loadContract(contractDescription: KtContractEffectList): FirContractDescription? {
        val effects: List<ConeEffectDeclaration> =
            contractDescription.getStubOrPsiChildren(
                KtStubElementTypes.CONTRACT_EFFECT,
                KtStubElementTypes.CONTRACT_EFFECT.arrayFactory
            ).mapNotNull { loadContractEffect(it) }
        if (effects.isEmpty()) return null
        return buildResolvedContractDescription {
            this.effects += effects.map { it.toFirEffectDeclaration() }
        }
    }

    private fun loadContractEffect(effect: KtContractEffect): ConeEffectDeclaration? {
        when (effect.stub?.effectType()) {
            EffectType.RETURNS_CONSTANT -> {
                val constExpr =
                    effect.getStubOrPsiChild(KtStubElementTypes.CONTRACT_EXPRESSION)?.stub ?: return null
                val ref =
                    if (constExpr.type() == ExpressionType.CONST && constExpr.data() == "WILDCARD") WILDCARD else loadExpression(constExpr) as? ConeConstantReference
                        ?: return null
                return ConeReturnsEffectDeclaration(ref)
            }
            EffectType.RETURNS_NOT_NULL -> ConeReturnsEffectDeclaration(NOT_NULL)
            EffectType.CALLS -> {
                val exprs = effect.getStubOrPsiChildren(
                    KtStubElementTypes.CONTRACT_EXPRESSION,
                    KtStubElementTypes.CONTRACT_EXPRESSION.arrayFactory
                )
                require(exprs.size == 2) { "$exprs" }
                val paramRef = exprs[0].stub
                require(paramRef != null && paramRef.type() == ExpressionType.PARAM) { "$paramRef" }
                val kindExpr = exprs[1].stub
                require(kindExpr != null && kindExpr.type() == ExpressionType.CONST) { "$kindExpr" }
                return ConeCallsEffectDeclaration(
                    loadParameterReference(paramRef.data()),
                    EventOccurrencesRange.valueOf(kindExpr.data())
                )
            }
            EffectType.CONDITIONAL -> {
                val conclusionStub = effect.getStubOrPsiChild(KtStubElementTypes.CONTRACT_EXPRESSION)?.stub
                require(conclusionStub != null && conclusionStub.type() == ExpressionType.CONCLUSION) { "$conclusionStub" }
                val exprStub = conclusionStub.childrenStubs[0]
                require(exprStub is KotlinContractExpressionStub) { "$exprStub" }
                val conclusion = loadExpression(exprStub) ?: return null
                val simpleEffect = effect.getStubOrPsiChild(KtStubElementTypes.CONTRACT_EFFECT) ?: return null
                val effectDeclaration = loadContractEffect(simpleEffect) ?: return null
                return ConeConditionalEffectDeclaration(effectDeclaration, conclusion)
            }
            else -> return null
        }
        return null
    }

    private fun loadExpression(constExpr: KotlinContractExpressionStub): ConeBooleanExpression? {
        when (constExpr.type()) {
            ExpressionType.CONST -> {
                return when (constExpr.data()) {
                    "TRUE" -> TRUE
                    "FALSE" -> FALSE
                    else -> return null
                }
            }
            ExpressionType.AND -> {
                return loadBooleanExpression(constExpr, LogicOperationKind.AND)
            }
            ExpressionType.OR -> {
                return loadBooleanExpression(constExpr, LogicOperationKind.OR)
            }
            ExpressionType.NOT -> {
                return ConeLogicalNot(
                    loadExpression(constExpr.childrenStubs[0] as KotlinContractExpressionStub) ?: return null
                )
            }
            ExpressionType.NULLABILITY -> {
                return ConeIsNullPredicate(
                    loadParameterReference(
                        (constExpr.childrenStubs[0] as KotlinContractExpressionStub).data()
                    ), constExpr.data().toBoolean()
                )
            }
            ExpressionType.PARAM -> {
                return loadParameterReference(constExpr.data()) as? ConeBooleanValueParameterReference
            }
            ExpressionType.IS_INSTANCE -> {
                val isNegated = constExpr.data().toBoolean()
                val paramRef =
                    constExpr.childrenStubs.find { it is KotlinContractExpressionStub } as? KotlinContractExpressionStub ?: return null
                val coneParamRef = loadParameterReference(paramRef.data())
                val typeReferences =
                    constExpr.getChildrenByType(KtStubElementTypes.TYPE_REFERENCE, KtStubElementTypes.TYPE_REFERENCE.arrayFactory)
                if (typeReferences.size != 1) return null
                return ConeIsInstancePredicate(coneParamRef, typeDeserializer.type(typeReferences[0]), isNegated)
            }
            ExpressionType.CONCLUSION -> error("Can't happen at this level")
            ExpressionType.NONE -> return null
        }
    }

    private fun loadParameterReference(data: String): ConeValueParameterReference {
        val paramIndex = Integer.parseInt(data)
        val param = if (paramIndex >= 0) simpleFunction.valueParameters[paramIndex] else null
        val paramName = param?.name?.asString() ?: "this"
        return if (param?.returnTypeRef?.isBoolean == true) {
            ConeBooleanValueParameterReference(paramIndex, paramName)
        } else ConeValueParameterReference(paramIndex, paramName)
    }

    private fun loadBooleanExpression(
        constExpr: KotlinContractExpressionStub,
        kind: LogicOperationKind
    ): ConeBooleanExpression? {
        val operands = constExpr.childrenStubs
        if (operands.size < 2) return null
        return operands.map { op -> loadExpression(op as KotlinContractExpressionStub) ?: return null }
            .reduce { acc, booleanExpression -> ConeBinaryLogicExpression(acc, booleanExpression, kind) }
    }
}