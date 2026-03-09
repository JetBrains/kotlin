/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeLocalEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeScopedCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.contextParametersForFunctionOrContainingProperty

data class Locality(
    val hasLocalContract: Boolean = false,
    val hasLocallyScopedContract: Boolean = false,
    val invocationKind: EventOccurrencesRange? = null,
)

val FirValueParameter.locality: Locality
    get() = computeLocality(containingDeclarationSymbol.fir)

val FirReceiverParameter.locality: Locality
    get() = computeLocality(containingDeclarationSymbol.fir)

internal fun FirDeclaration.computeLocality(containingDeclaration: FirDeclaration): Locality {
    val function = containingDeclaration as? FirCallableDeclaration ?: return Locality()
    val valueParameters = (containingDeclaration as? FirFunction)?.valueParameters.orEmpty()

    var hasLocalContract = false
    var hasLocallyScopedContract = false
    var invocationKind: EventOccurrencesRange? = null

    val contract = (containingDeclaration as? FirContractDescriptionOwner)?.contractDescription
    val index = when (this) {
        is FirReceiverParameter -> -1
        is FirValueParameter if this in valueParameters -> valueParameters.indexOf(this)
        is FirValueParameter if this in function.contextParametersForFunctionOrContainingProperty() ->
            function.contextParametersForFunctionOrContainingProperty().indexOf(this) + valueParameters.size
        else -> null
    }

    contract?.effects?.forEach { firEffect ->
        when (val effect = firEffect.effect) {
            is ConeLocalEffectDeclaration if effect.valueParameterReference.parameterIndex == index -> {
                hasLocalContract = true
            }
            is ConeScopedCallsEffectDeclaration if effect.valueParameterReference.parameterIndex == index -> {
                hasLocallyScopedContract = true
            }
            is ConeCallsEffectDeclaration if effect.valueParameterReference.parameterIndex == index -> {
                hasLocalContract = true
                invocationKind = effect.kind
            }
        }
    }

    return Locality(hasLocalContract, hasLocallyScopedContract, invocationKind)
}
