/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.name.CallableId

/**
 * This function generates a new function with [callableId] by copying [original]. It sets the [resolvePhase] of the copied
 * function as [firResolvePhase]. It sets the same [resolvePhase] for its value parameters and type parameters. It sets the
 * [origin] of the generated function as [origin] of [key]. It runs [extraInit] to support the additional custom initialization.
 */
public fun copyFirFunctionWithResolvePhase(
    original: FirSimpleFunction,
    callableId: CallableId,
    key: GeneratedDeclarationKey,
    firResolvePhase: FirResolvePhase,
    extraInit: FirSimpleFunctionBuilder.() -> Unit
): FirSimpleFunction = buildSimpleFunctionCopy(original) {
    symbol = FirNamedFunctionSymbol(callableId)
    origin = key.origin
    resolvePhase = firResolvePhase

    /**
     * Clears all elements and copies the elements of [originalParameters] to fill the given parameter list.
     * It matches `origin` and `resolvePhase` of  each copied element to the function.
     */
    fun MutableList<FirValueParameter>.copyFrom(originalParameters: List<FirValueParameter>) {
        clear()
        originalParameters.mapTo(this) { parameter ->
            buildValueParameterCopy(parameter) {
                symbol = FirValueParameterSymbol(name)
                containingFunctionSymbol = this@buildSimpleFunctionCopy.symbol
                origin = key.origin
                resolvePhase = firResolvePhase
            }
        }
    }

    // Match `origin` and `resolvePhase` of value parameters to the function.
    valueParameters.copyFrom(original.valueParameters)

    // Match `origin` and `resolvePhase` of type parameters to the function.
    typeParameters.clear()
    original.typeParameters.mapTo(typeParameters) { typeParameter ->
        buildTypeParameterCopy(typeParameter) {
            symbol = FirTypeParameterSymbol()
            containingDeclarationSymbol = this@buildSimpleFunctionCopy.symbol
            origin = key.origin
            resolvePhase = firResolvePhase
        }
    }

    extraInit()
}