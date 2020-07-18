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
import org.jetbrains.kotlin.fir.declarations.isInline
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

object InvocationKindTransformer : FirTransformer<Nothing?>() {
    private object ArgumentsTransformer : FirTransformer<Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>>() {
        override fun <E : FirElement> transformElement(element: E, data: Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformAnonymousFunction(
            anonymousFunction: FirAnonymousFunction,
            data: Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>
        ): CompositeTransformResult<FirStatement> {
            val kind = data.second ?: data.first[anonymousFunction]
            if (kind != null) {
                anonymousFunction.replaceInvocationKind(kind)
            }
            return anonymousFunction.compose()
        }

        override fun transformLambdaArgumentExpression(
            lambdaArgumentExpression: FirLambdaArgumentExpression,
            data: Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>
        ): CompositeTransformResult<FirStatement> {
            return data.first[lambdaArgumentExpression]?.let {
                (lambdaArgumentExpression.transformChildren(this, data.first to it) as FirStatement).compose()
            } ?: lambdaArgumentExpression.compose()
        }

        override fun transformNamedArgumentExpression(
            namedArgumentExpression: FirNamedArgumentExpression,
            data: Pair<Map<FirExpression, EventOccurrencesRange>, EventOccurrencesRange?>
        ): CompositeTransformResult<FirStatement> {
            return data.first[namedArgumentExpression]?.let {
                (namedArgumentExpression.transformChildren(this, data.first to it) as FirStatement).compose()
            } ?: namedArgumentExpression.compose()
        }
    }

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    override fun transformFunctionCall(functionCall: FirFunctionCall, data: Nothing?): CompositeTransformResult<FirStatement> {
        val calleeReference = functionCall.calleeReference as? FirNamedReferenceWithCandidate ?: return functionCall.compose()
        val argumentMapping = calleeReference.candidate.argumentMapping ?: return functionCall.compose()
        val function = calleeReference.candidateSymbol.fir as? FirSimpleFunction ?: return functionCall.compose()

        val callsEffects = function.contractDescription.effects
            ?.map { it.effect }
            ?.filterIsInstance<ConeCallsEffectDeclaration>() ?: emptyList()

        val isInline = function.isInline
        if (callsEffects.isEmpty() && !isInline) {
            return functionCall.compose()
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
            return functionCall.compose()
        }
        functionCall.argumentList.transformArguments(ArgumentsTransformer, invocationKindMapping to null)
        return functionCall.compose()
    }
}