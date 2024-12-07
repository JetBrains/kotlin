/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

class FirDeclarationOverloadabilityHelperImpl(val session: FirSession) : FirDeclarationOverloadabilityHelper {
    override fun isOverloadable(a: FirCallableSymbol<*>, b: FirCallableSymbol<*>): Boolean {
        val sigA = createSignature(a)
        val sigB = createSignature(b)

        return !(isEquallyOrMoreSpecific(sigA, sigB) && isEquallyOrMoreSpecific(sigB, sigA))
    }

    override fun isEquallyOrMoreSpecific(
        sigA: FlatSignature<FirCallableSymbol<*>>,
        sigB: FlatSignature<FirCallableSymbol<*>>,
    ): Boolean = createEmptyConstraintSystem().isSignatureEquallyOrMoreSpecific(
        sigA,
        sigB,
        OverloadabilitySpecificityCallbacks,
        session.typeSpecificityComparatorProvider?.typeSpecificityComparator ?: TypeSpecificityComparator.NONE,
    )

    override fun createSignature(declaration: FirCallableSymbol<*>): FlatSignature<FirCallableSymbol<*>> {
        val valueParameters = (declaration as? FirFunctionSymbol<*>)?.valueParameterSymbols.orEmpty()

        return FlatSignature(
            origin = declaration,
            typeParameters = declaration.typeParameterSymbols.map { it.toLookupTag() },
            valueParameterTypes = buildList<KotlinTypeMarker> {
                declaration.resolvedContextParameters.mapTo(this) { it.returnTypeRef.coneType }
                declaration.receiverParameter?.let { add(it.typeRef.coneType) }
                valueParameters.mapTo(this) { it.resolvedReturnType }
            },
            hasExtensionReceiver = declaration.receiverParameter != null,
            contextReceiverCount = declaration.resolvedContextParameters.size,
            hasVarargs = valueParameters.any { it.isVararg },
            numDefaults = 0,
            isExpect = declaration.isExpect,
            isSyntheticMember = declaration.origin is FirDeclarationOrigin.Synthetic
        )
    }

    /**
     * See [org.jetbrains.kotlin.resolve.calls.results.createForPossiblyShadowedExtension]
     */
    override fun createSignatureForPossiblyShadowedExtension(declaration: FirCallableSymbol<*>): FlatSignature<FirCallableSymbol<*>> {
        val valueParameters = (declaration as? FirFunctionSymbol<*>)?.valueParameterSymbols.orEmpty()

        return FlatSignature(
            origin = declaration,
            typeParameters = declaration.typeParameterSymbols.map { it.toLookupTag() },
            valueParameterTypes = valueParameters.map { it.resolvedReturnType },
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = valueParameters.any { it.isVararg },
            numDefaults = valueParameters.count { it.hasDefaultValue },
            isExpect = declaration.isExpect,
            isSyntheticMember = declaration.origin is FirDeclarationOrigin.Synthetic
        )
    }

    private fun createEmptyConstraintSystem(): SimpleConstraintSystem {
        return ConeSimpleConstraintSystemImpl(session.inferenceComponents.createConstraintSystem(), session)
    }
}
