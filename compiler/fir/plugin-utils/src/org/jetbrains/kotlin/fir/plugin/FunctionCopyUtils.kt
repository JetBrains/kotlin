/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.builder.buildFirList
import org.jetbrains.kotlin.fir.declarations.FirDeclarationStatus
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.name.CallableId

/**
 * This function generates a new function with [callableId] by copying [original]. It sets the [resolvePhase] of the copied
 * function as [firResolvePhase]. It sets the same [resolvePhase] for its value parameters and type parameters. It sets the
 * [origin] of the generated function as [origin] of [key].
 */
public fun copyFirFunctionWithResolvePhase(
    original: FirNamedFunction,
    callableId: CallableId,
    key: GeneratedDeclarationKey,
    firResolvePhase: FirResolvePhase,
    status: FirDeclarationStatus,
): FirNamedFunction {
    val newFunctionSymbol = FirNamedFunctionSymbol(callableId)

    /**
     * Copies the receiver parameters.
     * It matches `origin` and `resolvePhase` of each copied element to the function.
     */
    fun List<FirValueParameter>.copy(): MutableList<FirValueParameter> {
        return buildFirList {
            this@copy.mapTo(this) { parameter ->
                buildValueParameterCopy(
                    parameter,
                    symbol = FirValueParameterSymbol(),
                    containingDeclarationSymbol = newFunctionSymbol,
                    origin = key.origin,
                    resolvePhase = firResolvePhase,
                )
            }
        }
    }

    return buildNamedFunctionCopy(
        original,
        symbol = newFunctionSymbol,
        origin = key.origin,
        resolvePhase = firResolvePhase,
        status = status,

        // Match `origin` and `resolvePhase` of the receiver parameter to the function.
        receiverParameter = original.receiverParameter?.let { receiverParameter ->
            buildReceiverParameterCopy(
                receiverParameter,
                symbol = FirReceiverParameterSymbol(),
                containingDeclarationSymbol = newFunctionSymbol,
                origin = key.origin,
                resolvePhase = firResolvePhase,
            )
        },

        // Match `origin` and `resolvePhase` of context receivers to the function.
        contextParameters = original.contextParameters.copy(),

        // Match `origin` and `resolvePhase` of value parameters to the function.
        valueParameters = original.valueParameters.copy(),

        // Match `origin` and `resolvePhase` of type parameters to the function.
        typeParameters = buildFirList {
            original.typeParameters.mapTo(this) {
                buildTypeParameterCopy(
                    it,
                    symbol = FirTypeParameterSymbol(),
                    containingDeclarationSymbol = newFunctionSymbol,
                    origin = key.origin,
                    resolvePhase = firResolvePhase,
                )
            }
        }
    )
}
