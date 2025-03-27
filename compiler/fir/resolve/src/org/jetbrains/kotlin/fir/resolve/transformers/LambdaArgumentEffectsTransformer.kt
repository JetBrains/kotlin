/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.KtBooleanValueParameterReference
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeHoldsInEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.lambdaArgumentHoldsInTruths
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNonReflectFunctionType
import org.jetbrains.kotlin.fir.unwrapFakeOverrides

tailrec fun FirExpression.unwrapAnonymousFunctionExpression(): FirAnonymousFunction? = when (this) {
    is FirAnonymousFunctionExpression -> anonymousFunction
    is FirWrappedArgumentExpression -> expression.unwrapAnonymousFunctionExpression()
    else -> null
}

fun FirFunctionCall.replaceLambdaArgumentEffects(session: FirSession) {
    val calleeReference = calleeReference as? FirNamedReferenceWithCandidate ?: return
    val argumentMapping = calleeReference.candidate.argumentMapping
    val symbol = calleeReference.candidate.symbol
    val function = (symbol.fir as? FirSimpleFunction) ?: (symbol.fir as? FirConstructor) ?: return
    val isInline = function.isInline || symbol.isArrayConstructorWithLambda

    // Candidate could be a substitution or intersection fake override; unwrap and get the effects of the base function.
    val effects = (function.unwrapFakeOverrides<FirFunction>() as? FirContractDescriptionOwner)?.contractDescription?.effects

    val eventOccurencesRangeByParameter = mutableMapOf<FirValueParameter, EventOccurrencesRange>()
    val conditionParameterByParameter = mutableMapOf<FirValueParameter, FirValueParameter>()
    effects?.forEach { fir ->
        when (val effect = fir.effect) {
            is ConeCallsEffectDeclaration -> {
                // TODO: Support callsInPlace contracts on receivers, KT-59681
                function.valueParameters.getOrNull(effect.valueParameterReference.parameterIndex)?.let { valueParameter ->
                    eventOccurencesRangeByParameter[valueParameter] = effect.kind
                }
            }
            is ConeHoldsInEffectDeclaration -> {
                val conditionParameterIndex = (effect.argumentsCondition as? KtBooleanValueParameterReference)?.parameterIndex
                if (conditionParameterIndex != null) {
                    val conditionParameter = function.valueParameters.getOrNull(conditionParameterIndex)
                    val lambdaParameter = function.valueParameters.getOrNull(effect.valueParameterReference.parameterIndex)
                    if (conditionParameter != null && lambdaParameter != null) {
                        conditionParameterByParameter[lambdaParameter] = conditionParameter
                    }
                }
            }
        }
    }
    if (eventOccurencesRangeByParameter.isEmpty() && !isInline) return

    for ((argument, parameter) in argumentMapping) {
        val lambda = argument.expression.unwrapAnonymousFunctionExpression() ?: continue
        lambda.transformInlineStatus(parameter, isInline, session)
        val kind = eventOccurencesRangeByParameter[parameter] ?: EventOccurrencesRange.UNKNOWN.takeIf {
            // Inline functional parameters have to be called in-place; that's the only permitted operation on them.
            isInline && !parameter.isNoinline && !parameter.isCrossinline &&
                    parameter.returnTypeRef.coneType.isNonReflectFunctionType(session)
        }
        if (kind != null) {
            lambda.replaceInvocationKind(kind)
        }
        conditionParameterByParameter[parameter]?.let { conditionParameter ->
            val conditionExpression = argumentMapping.entries.find { (_, parameter) -> parameter == conditionParameter }?.key?.expression
            if (conditionExpression != null) {
                lambda.lambdaArgumentHoldsInTruths = lambda.lambdaArgumentHoldsInTruths.orEmpty() + conditionExpression
            }
        }
    }
}
