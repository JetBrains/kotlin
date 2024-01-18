/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScopeContext
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

abstract class CallableCopyTypeCalculator {
    abstract fun computeReturnType(declaration: FirCallableDeclaration): FirTypeRef?

    object DoNothing : CallableCopyTypeCalculator() {
        override fun computeReturnType(declaration: FirCallableDeclaration): FirTypeRef {
            return declaration.returnTypeRef
        }
    }

    abstract class AbstractCallableCopyTypeCalculator : CallableCopyTypeCalculator() {
        override fun computeReturnType(declaration: FirCallableDeclaration): FirResolvedTypeRef? {
            val callableCopyDeferredTypeCalculation = declaration.attributes.callableCopyDeferredTypeCalculation
                ?: return declaration.getResolvedTypeRef()

            // TODO: drop synchronized in KT-60385
            synchronized(callableCopyDeferredTypeCalculation) {
                if (declaration.attributes.callableCopyDeferredTypeCalculation == null) {
                    return declaration.returnTypeRef as FirResolvedTypeRef
                }

                val returnType = when (callableCopyDeferredTypeCalculation) {
                    is CallableCopySubstitution -> computeSubstitution(callableCopyDeferredTypeCalculation)
                    is CallableCopyIntersection -> computeIntersection(callableCopyDeferredTypeCalculation)
                } ?: return null
                val returnTypeRef = declaration.returnTypeRef.resolvedTypeFromPrototype(returnType)

                declaration.replaceReturnTypeRef(returnTypeRef)
                if (declaration is FirProperty) {
                    declaration.getter?.replaceReturnTypeRef(returnTypeRef)
                    declaration.setter?.valueParameters?.firstOrNull()?.replaceReturnTypeRef(returnTypeRef)
                }

                declaration.attributes.callableCopyDeferredTypeCalculation = null

                return returnTypeRef
            }
        }

        private fun computeSubstitution(callableCopySubstitutionForTypeUpdater: CallableCopySubstitution): ConeKotlinType? {
            val (substitutor, baseSymbol) = callableCopySubstitutionForTypeUpdater
            val baseDeclaration = baseSymbol.fir as FirCallableDeclaration
            val baseReturnType = computeReturnType(baseDeclaration)?.type ?: return null
            val coneType = substitutor.substituteOrSelf(baseReturnType)

            return coneType
        }

        private fun computeIntersection(callableCopyIntersection: CallableCopyIntersection): ConeKotlinType? {
            val (mostSpecific, session) = callableCopyIntersection
            return FirTypeIntersectionScopeContext.intersectReturnTypes(mostSpecific, session) {
                computeReturnType(this)?.type
            }
        }

        protected abstract fun FirCallableDeclaration.getResolvedTypeRef(): FirResolvedTypeRef?
    }


    object Forced : AbstractCallableCopyTypeCalculator() {
        override fun FirCallableDeclaration.getResolvedTypeRef(): FirResolvedTypeRef? {
            return returnTypeRef as? FirResolvedTypeRef
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------------

private object CallableCopyDeferredReturnTypeCalculationKey : FirDeclarationDataKey()

var FirDeclarationAttributes.callableCopyDeferredTypeCalculation: CallableCopyDeferredReturnTypeCalculation? by FirDeclarationDataRegistry.attributesAccessor(
    CallableCopyDeferredReturnTypeCalculationKey
)

sealed class CallableCopyDeferredReturnTypeCalculation

data class CallableCopySubstitution internal constructor(
    val substitutor: ConeSubstitutor,
    val baseSymbol: FirBasedSymbol<*>
) : CallableCopyDeferredReturnTypeCalculation()

data class CallableCopyIntersection internal constructor(
    val mostSpecific: Collection<FirCallableSymbol<*>>,
    val session: FirSession,
) : CallableCopyDeferredReturnTypeCalculation() {
    override fun toString(): String {
        return "CallableCopyIntersection(mostSpecific=$mostSpecific)"
    }
}