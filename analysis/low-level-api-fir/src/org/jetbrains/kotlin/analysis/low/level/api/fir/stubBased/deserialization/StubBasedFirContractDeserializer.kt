/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
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
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.stubs.impl.*

class StubBasedFirContractDeserializer(
    private val simpleFunction: FirSimpleFunction,
    private val typeDeserializer: StubBasedFirTypeDeserializer
) {
    fun loadContract(function: KtNamedFunction): FirContractDescription? {
        val functionStub = function.stub as? KotlinFunctionStubImpl ?: return null
        val effects = functionStub.contract?.mapNotNull { loadContractEffect(it) }

        if (effects.isNullOrEmpty()) return null
        return buildResolvedContractDescription {
            this.effects += effects.map { it.toFirEffectDeclaration() }
        }
    }

    private fun loadContractEffect(effect: Effect, loadConclusion: Boolean = true): ConeEffectDeclaration? {
        if (loadConclusion) {
            val conclusion = effect.conclusion
            if (conclusion != null) {
                val conclusionExpression = loadExpression(conclusion) ?: return null
                val contractEffectDeclaration = loadContractEffect(effect, false) ?: return null
                return ConeConditionalEffectDeclaration(contractEffectDeclaration, conclusionExpression)
            }
        }
        when (effect.effectType) {
            EffectType.RETURNS_CONSTANT -> {
                val args =
                    effect.arguments ?: return null
                val constExpr = args.firstOrNull()
                val ref =
                    if (constExpr == null) WILDCARD else loadExpression(constExpr) as? ConeConstantReference
                        ?: return null
                return ConeReturnsEffectDeclaration(ref)
            }
            EffectType.RETURNS_NOT_NULL -> ConeReturnsEffectDeclaration(NOT_NULL)
            EffectType.CALLS -> {
                val exprs = effect.arguments ?: return null
                val argument = exprs.firstOrNull() ?: return null
                return ConeCallsEffectDeclaration(
                    loadParameterReference(argument.valueParameter ?: return null),
                    effect.invocationKind?.toEventOccurrencesRange() ?: EventOccurrencesRange.UNKNOWN
                )
            }
            else -> return null
        }
        return null
    }

    private fun loadExpression(constExpr: Expression): ConeBooleanExpression? {
        when {
            constExpr.constantValue != null -> {
                return when (constExpr.constantValue) {
                    ContractConstantValue.TRUE -> TRUE
                    ContractConstantValue.FALSE -> FALSE
                    else -> return null
                }
            }
            constExpr.andArgs != null -> {
                return loadBooleanExpression(constExpr, LogicOperationKind.AND)
            }
            constExpr.orArgs != null -> {
                return loadBooleanExpression(constExpr, LogicOperationKind.OR)
            }
            constExpr.isInNullPredicate -> {
                return ConeIsNullPredicate(
                    loadParameterReference(
                        constExpr.valueParameter!!
                    ), constExpr.isNegated
                )
            }
            constExpr.type != null -> {
                val isNegated = constExpr.isNegated
                val coneParamRef = loadParameterReference(constExpr.valueParameter!!)
                val kotlinType = typeDeserializer.type(constExpr.type!!) ?: return null
                return ConeIsInstancePredicate(coneParamRef, kotlinType, isNegated)
            }
            constExpr.valueParameter != null -> {
                val paramIndex = constExpr.valueParameter!!
                return loadParameterReference(paramIndex) as? ConeBooleanValueParameterReference
            }
            constExpr.isNegated -> {
                return ConeLogicalNot(
                    loadExpression(constExpr.copy(isNegated = false)) ?: return null
                )
            }
            else -> error("Unknown expression $constExpr")
        }
    }

    private fun loadParameterReference(paramIndex: Int): ConeValueParameterReference {
        val param = if (paramIndex >= 0) simpleFunction.valueParameters[paramIndex] else null
        val paramName = param?.name?.asString() ?: "this"
        return if (param?.returnTypeRef?.isBoolean == true) {
            ConeBooleanValueParameterReference(paramIndex, paramName)
        } else ConeValueParameterReference(paramIndex, paramName)
    }

    private fun loadBooleanExpression(
        constExpr: Expression,
        kind: LogicOperationKind
    ): ConeBooleanExpression? {
        val operands = when (kind){
            LogicOperationKind.AND -> constExpr.andArgs
            LogicOperationKind.OR -> constExpr.orArgs
        } ?: return null
        if (operands.size < 2) return null
        return operands.map { op -> loadExpression(op) ?: return null }
            .reduce { acc, booleanExpression -> ConeBinaryLogicExpression(acc, booleanExpression, kind) }
    }
}