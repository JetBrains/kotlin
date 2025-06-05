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
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

// 16 is enough to compare two CP lists with 4 types each.
private const val MAX_COMPLEXITY_FOR_CONTEXT_PARAMETERS = 16

class FirDeclarationOverloadabilityHelperImpl(val session: FirSession) : FirDeclarationOverloadabilityHelper {
    override fun isConflicting(a: FirCallableSymbol<*>, b: FirCallableSymbol<*>, ignoreContextParameters: Boolean): Boolean {
        val sigA = createSignature(a, ignoreContextParameters)
        val sigB = createSignature(b, ignoreContextParameters)

        return isEquallyOrMoreSpecific(sigA, sigB) && isEquallyOrMoreSpecific(sigB, sigA)
    }

    override fun getContextParameterShadowing(
        a: FirCallableSymbol<*>,
        b: FirCallableSymbol<*>,
    ): FirDeclarationOverloadabilityHelper.ContextParameterShadowing {
        // bShadowsA && aShadowsB => BothWays
        // bShadowsA && !aShadowsB => Shadowing
        // else                   => None

        val bShadowsA = isShadowingContextParameters(b, a)
        // Early return to skip needless computation of aShadowsB
        if (!bShadowsA) return FirDeclarationOverloadabilityHelper.ContextParameterShadowing.None

        val aShadowsB = isShadowingContextParameters(a, b)

        return if (aShadowsB) {
            FirDeclarationOverloadabilityHelper.ContextParameterShadowing.BothWays
        } else {
            FirDeclarationOverloadabilityHelper.ContextParameterShadowing.Shadowing
        }
    }

    private fun isShadowingContextParameters(
        a: FirCallableSymbol<*>,
        b: FirCallableSymbol<*>,
    ): Boolean {
        // The complexity of this check is O(a.contextParameterSymbols.size * b.contextParameterSymbols.size),
        // to limit quadratic explosion, we only check below a certain threshold.
        if (a.contextParameterSymbols.size * b.contextParameterSymbols.size > MAX_COMPLEXITY_FOR_CONTEXT_PARAMETERS) {
            return false
        }

        // A is shadowing B if for every type in A's context parameter list, there is a type in B's context parameter list
        // that is equally or more specific.

        fun singleTypeSignature(type: ConeKotlinType, symbol: FirCallableSymbol<*>): FlatSignature<FirCallableSymbol<*>> {
            return FlatSignature(
                origin = symbol,
                typeParameters = symbol.typeParameterSymbols.map { it.toLookupTag() },
                valueParameterTypes = listOf(type),
                hasExtensionReceiver = false,
                contextReceiverCount = 0,
                hasVarargs = false,
                numDefaults = 0,
                isExpect = false,
                isSyntheticMember = false
            )
        }

        val bSingleTypeSignatures = b.contextParameterSymbols.map { singleTypeSignature(it.resolvedReturnType, b) }

        return a.contextParameterSymbols.all {
            val aType = singleTypeSignature(it.resolvedReturnType, a)
            bSingleTypeSignatures.any { bType ->
                isEquallyOrMoreSpecific(bType, aType)
            }
        }
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

    override fun createSignature(declaration: FirCallableSymbol<*>, ignoreContextParameters: Boolean): FlatSignature<FirCallableSymbol<*>> {
        val valueParameters = (declaration as? FirFunctionSymbol<*>)?.valueParameterSymbols.orEmpty()

        return FlatSignature(
            origin = declaration,
            typeParameters = declaration.typeParameterSymbols.map { it.toLookupTag() },
            valueParameterTypes = buildList<KotlinTypeMarker> {
                if (!ignoreContextParameters) {
                    declaration.contextParameterSymbols.mapTo(this) { it.resolvedReturnType }
                }
                declaration.resolvedReceiverType?.let { add(it) }
                valueParameters.mapTo(this) { it.resolvedReturnType }
            },
            hasExtensionReceiver = declaration.receiverParameterSymbol != null,
            contextReceiverCount = if (ignoreContextParameters) 0 else declaration.contextParameterSymbols.size,
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
            valueParameterTypes = buildList<KotlinTypeMarker> {
                declaration.contextParameterSymbols.mapTo(this) { it.resolvedReturnType }
                valueParameters.mapTo(this) { it.resolvedReturnType }
            },
            hasExtensionReceiver = false,
            contextReceiverCount = declaration.contextParameterSymbols.size,
            hasVarargs = valueParameters.any { it.isVararg },
            numDefaults = 0,
            isExpect = declaration.isExpect,
            isSyntheticMember = declaration.origin is FirDeclarationOrigin.Synthetic
        )
    }

    private fun createEmptyConstraintSystem(): SimpleConstraintSystem {
        return ConeSimpleConstraintSystemImpl(session.inferenceComponents.createConstraintSystem(), session)
    }
}
