/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOverloadabilityHelper
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOverloadabilityHelper.ContextParameterShadowing
import org.jetbrains.kotlin.fir.declarations.typeSpecificityComparatorProvider
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.inference.inferenceLogger
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.resolve.calls.inference.runTransaction
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

// 16 is enough to compare two CP lists with 4 types each.
private const val MAX_COMPLEXITY_FOR_CONTEXT_PARAMETERS = 16

class FirDeclarationOverloadabilityHelperImpl(val session: FirSession) : FirDeclarationOverloadabilityHelper {
    override fun isConflicting(a: FirCallableSymbol<*>, b: FirCallableSymbol<*>): Boolean {
        val sigA = createSignature(a, ignoreContextParameters = false)
        val sigB = createSignature(b, ignoreContextParameters = false)

        return isEquallyOrMoreSpecific(sigA, sigB) && isEquallyOrMoreSpecific(sigB, sigA)
    }

    override fun getContextParameterShadowing(
        a: FirCallableSymbol<*>,
        b: FirCallableSymbol<*>,
    ): ContextParameterShadowing {
        val sigA = createSignature(a, ignoreContextParameters = true)
        val sigB = createSignature(b, ignoreContextParameters = true)

        val csB = createEmptyConstraintSystem()
        val stateB = csB.signatureComparisonStateIfEquallyOrMoreSpecific(sigA, sigB) ?: return ContextParameterShadowing.None

        val csA = createEmptyConstraintSystem()
        val stateA = csA.signatureComparisonStateIfEquallyOrMoreSpecific(sigB, sigA) ?: return ContextParameterShadowing.None

        // The complexity of this check is O(a.contextParameterSymbols.size * b.contextParameterSymbols.size),
        // to limit quadratic explosion, we only check below a certain threshold.
        if (a.contextParameterSymbols.size * b.contextParameterSymbols.size > MAX_COMPLEXITY_FOR_CONTEXT_PARAMETERS) {
            return ContextParameterShadowing.None
        }

        // bShadowsA && aShadowsB => BothWays
        // bShadowsA && !aShadowsB => Shadowing
        // else                   => None

        val bShadowsA = isShadowingContextParameters(b, a, csB, stateB)
        // Early return to skip needless computation of aShadowsB
        if (!bShadowsA) return ContextParameterShadowing.None

        val aShadowsB = isShadowingContextParameters(a, b, csA, stateA)

        return if (aShadowsB) {
            ContextParameterShadowing.BothWays
        } else {
            ContextParameterShadowing.Shadowing
        }
    }

    override fun isExtensionShadowedByMember(extension: FirCallableSymbol<*>, member: FirCallableSymbol<*>): Boolean {
        val sigExtension = createSignatureForPossiblyShadowedExtension(extension)
        val sigMember = createSignature(member, ignoreContextParameters = true)

        val cs = createEmptyConstraintSystem()
        val state = cs.signatureComparisonStateIfEquallyOrMoreSpecific(sigExtension, sigMember) ?: return false

        // The complexity of this check is O(a.contextParameterSymbols.size * b.contextParameterSymbols.size),
        // to limit quadratic explosion, we only check below a certain threshold.
        if (extension.contextParameterSymbols.size * member.contextParameterSymbols.size > MAX_COMPLEXITY_FOR_CONTEXT_PARAMETERS) {
            return false
        }

        return isShadowingContextParameters(member, extension, cs, state)
    }

    private fun isShadowingContextParameters(
        a: FirCallableSymbol<*>,
        b: FirCallableSymbol<*>,
        cs: ConeSimpleConstraintSystemImpl,
        state: FlatSignatureComparisonState,
    ): Boolean {
        // A is shadowing B if for every type in A's context parameter list, there is a type in B's context parameter list
        // that is equally or more specific.

        return a.contextParameterSymbols.all { cpA ->
            b.contextParameterSymbols.any { cpB ->
                cs.system.runTransaction {
                    !state.isLessSpecific(cpB.resolvedReturnType, cpA.resolvedReturnType)
                }
            }
        }
    }

    private fun isEquallyOrMoreSpecific(
        sigA: FlatSignature<FirCallableSymbol<*>>,
        sigB: FlatSignature<FirCallableSymbol<*>>,
    ): Boolean = createEmptyConstraintSystem().signatureComparisonStateIfEquallyOrMoreSpecific(sigA, sigB) != null

    private fun createSignature(declaration: FirCallableSymbol<*>, ignoreContextParameters: Boolean): FlatSignature<FirCallableSymbol<*>> {
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
    private fun createSignatureForPossiblyShadowedExtension(declaration: FirCallableSymbol<*>): FlatSignature<FirCallableSymbol<*>> {
        val valueParameters = (declaration as? FirFunctionSymbol<*>)?.valueParameterSymbols.orEmpty()

        return FlatSignature(
            origin = declaration,
            typeParameters = declaration.typeParameterSymbols.map { it.toLookupTag() },
            valueParameterTypes = buildList<KotlinTypeMarker> {
                valueParameters.mapTo(this) { it.resolvedReturnType }
            },
            hasExtensionReceiver = false,
            contextReceiverCount = 0,
            hasVarargs = valueParameters.any { it.isVararg },
            numDefaults = 0,
            isExpect = declaration.isExpect,
            isSyntheticMember = declaration.origin is FirDeclarationOrigin.Synthetic
        )
    }

    private fun createEmptyConstraintSystem(): ConeSimpleConstraintSystemImpl {
        return ConeSimpleConstraintSystemImpl(session.inferenceComponents.createConstraintSystem(), session).also {
            session.inferenceLogger?.logStage("Some isEquallyOrMoreSpecific() call", it.constraintSystemMarker)
        }
    }

    private fun ConeSimpleConstraintSystemImpl.signatureComparisonStateIfEquallyOrMoreSpecific(
        sigA: FlatSignature<FirCallableSymbol<*>>,
        sigB: FlatSignature<FirCallableSymbol<*>>,
    ): FlatSignatureComparisonState? {
        return signatureComparisonStateIfEquallyOrMoreSpecific(
            sigA,
            sigB,
            OverloadabilitySpecificityCallbacks,
            session.typeSpecificityComparatorProvider?.typeSpecificityComparator ?: TypeSpecificityComparator.NONE
        )
    }
}
