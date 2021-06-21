/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.visitors.FirTransformer

object InvocationKindTransformer : FirTransformer<Any?>() {
    private object ArgumentsTransformer : FirTransformer<Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>>() {
        override fun <E : FirElement> transformElement(element: E, data: Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>): E {
            return element
        }

        override fun transformAnonymousFunctionExpression(
            anonymousFunctionExpression: FirAnonymousFunctionExpression,
            data: Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>
        ): FirStatement {
            val kind = data.second ?: data.first[anonymousFunctionExpression]
            return anonymousFunctionExpression.transformAnonymousFunction(this, emptyMap<FirExpression, EventOccurrencesRange>() to kind)
        }

        override fun transformAnonymousFunction(
            anonymousFunction: FirAnonymousFunction,
            data: Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>
        ): FirStatement {
            val kind = data.second
            if (kind != null) {
                anonymousFunction.replaceInvocationKind(kind)
            }
            return anonymousFunction
        }

        override fun transformLambdaArgumentExpression(
            lambdaArgumentExpression: FirLambdaArgumentExpression,
            data: Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>
        ): FirStatement {
            return data.first[lambdaArgumentExpression]?.let {
                (lambdaArgumentExpression.transformChildren(this, data.first to it) as FirStatement)
            } ?: lambdaArgumentExpression
        }

        override fun transformNamedArgumentExpression(
            namedArgumentExpression: FirNamedArgumentExpression,
            data: Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>
        ): FirStatement {
            return data.first[namedArgumentExpression]?.let {
                (namedArgumentExpression.transformChildren(this, data.first to it) as FirStatement)
            } ?: namedArgumentExpression
        }
    }

    override fun <E : FirElement> transformElement(element: E, data: Any?): E {
        return element
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Any?): FirStatement {
        val calleeReference = functionCall.calleeReference as? FirNamedReferenceWithCandidate ?: return functionCall
        val argumentMapping = calleeReference.candidate.argumentMapping ?: return functionCall
        val function = calleeReference.candidateSymbol.fir as? FirSimpleFunction ?: return functionCall

        val callsEffects = function.contractDescription.effects
            ?.map { it.effect }
            ?.filterIsInstance<ConeCallsEffectDeclaration>() ?: emptyList()

        val isInline = function.isInline
        if (callsEffects.isEmpty() && !isInline) {
            return functionCall
        }

        val reversedArgumentMapping = argumentMapping.entries.map { (argument, parameter) ->
            parameter to argument
        }.toMap()

        val invocationKindMapping = mutableMapOf<FirExpression, EventOccurrencesRange>()
        for (effect in callsEffects) {
            // TODO: Support callsInPlace contracts on receivers
            val valueParameter = function.valueParameters.getOrNull(effect.valueParameterReference.parameterIndex) ?: continue
            val argument = reversedArgumentMapping[valueParameter] ?: continue
            invocationKindMapping[argument] = effect.kind
        }
        if (isInline) {
            for (argument in functionCall.arguments) {
                invocationKindMapping.putIfAbsent(argument, EventOccurrencesRange.UNKNOWN)
            }
        }
        if (invocationKindMapping.isEmpty()) {
            return functionCall
        }
        functionCall.argumentList.transformArguments(ArgumentsTransformer, invocationKindMapping to null)
        return functionCall
    }
}
