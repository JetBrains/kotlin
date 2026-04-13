/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeHoldsInEffectDeclaration
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.lambdaArgumentParent
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.unwrapAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirAbstractContractResolveTransformerDispatcher
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNonReflectFunctionType
import org.jetbrains.kotlin.fir.unwrapFakeOverrides

fun FirFunctionCall.replaceLambdaArgumentEffects(transformer: FirAbstractBodyResolveTransformer) {
    val calleeReference = calleeReference as? FirNamedReferenceWithCandidate ?: return
    val argumentMapping = calleeReference.candidate.argumentMapping
    val symbol = calleeReference.candidate.symbol
    val function = (symbol.fir as? FirNamedFunction) ?: (symbol.fir as? FirConstructor) ?: return
    val isInline = function.isInline || symbol.isArrayConstructorWithLambda

    // Recursive contracts are prohibited, so this check ensures that no contracts are applied during contract resolution
    val effects = if (transformer is FirAbstractContractResolveTransformerDispatcher && transformer.insideContractDescription) {
        emptyList()
    } else {
        // Candidate could be a substitution or intersection fake override; unwrap and get the effects of the base function.
        function.unwrapFakeOverrides<FirFunction>().symbol.resolvedContractDescription?.effects.orEmpty()
    }

    val eventOccurencesRangeByParameter = mutableMapOf<FirValueParameter, EventOccurrencesRange>()
    val lambdaParametersWithHoldsInEffect = mutableSetOf<FirValueParameter>()
    for (fir in effects) {
        when (val effect = fir.effect) {
            is ConeCallsEffectDeclaration -> {
                // TODO: Support callsInPlace contracts on receivers, KT-59681
                function.valueParameters.getOrNull(effect.valueParameterReference.parameterIndex)?.let { valueParameter ->
                    eventOccurencesRangeByParameter[valueParameter] = effect.kind
                }
            }
            is ConeHoldsInEffectDeclaration -> {
                val lambdaParameter = function.valueParameters.getOrNull(effect.valueParameterReference.parameterIndex)
                if (lambdaParameter != null) {
                    lambdaParametersWithHoldsInEffect += lambdaParameter
                }
            }
        }
    }

    if (eventOccurencesRangeByParameter.isEmpty() && !isInline) return

    val session = transformer.session
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
        if (lambdaParametersWithHoldsInEffect.contains(parameter)) {
            lambda.lambdaArgumentParent = this
        }
    }
}
