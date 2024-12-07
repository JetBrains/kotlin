/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeOrNull

/**
 * A utility class that, depending on the implementation, calculates the return type of callable copies,
 * (substitution/intersection overrides, delegated members, and enhanced Java declarations) and returns it.
 *
 * See [FirDeclarationAttributes.deferredCallableCopyReturnType].
 */
abstract class CallableCopyTypeCalculator {
    /**
     * Returns the [FirTypeRef] for [FirCallableDeclaration.returnTypeRef] of the [declaration].
     *
     * Depending on the implementation, this call might invoke a deferred computation of the return type
     * (see [FirDeclarationAttributes.deferredCallableCopyReturnType]).
     *
     * A return value of `null` signifies that the calculation has failed or that no deferred computation was stored
     * and the return type could not be resolved ordinarily.
     */
    abstract fun computeReturnType(declaration: FirCallableDeclaration): FirTypeRef?

    fun computeReturnTypeOrNull(declaration: FirCallableDeclaration): ConeKotlinType? {
        return computeReturnType(declaration)?.coneTypeOrNull
    }

    /**
     * Doesn't perform any calculation and returns [FirCallableDeclaration.returnTypeRef].
     */
    object DoNothing : CallableCopyTypeCalculator() {
        override fun computeReturnType(declaration: FirCallableDeclaration): FirTypeRef {
            return declaration.returnTypeRef
        }
    }

    /**
     * If necessary, runs the computation saved in [FirDeclarationAttributes.deferredCallableCopyReturnType] and returns a [FirResolvedTypeRef].
     */
    abstract class DeferredCallableCopyTypeCalculator : CallableCopyTypeCalculator() {
        override fun computeReturnType(declaration: FirCallableDeclaration): FirResolvedTypeRef? {
            val callableCopyDeferredTypeCalculation = declaration.attributes.deferredCallableCopyReturnType
                ?: return declaration.getResolvedTypeRef()

            // TODO: drop synchronized in KT-60385
            synchronized(callableCopyDeferredTypeCalculation) {
                if (declaration.attributes.deferredCallableCopyReturnType == null) {
                    return declaration.returnTypeRef as FirResolvedTypeRef
                }

                val returnType = callableCopyDeferredTypeCalculation.computeReturnType(this) ?: return null
                val returnTypeRef = declaration.returnTypeRef.resolvedTypeFromPrototype(returnType)

                declaration.replaceReturnTypeRef(returnTypeRef)
                if (declaration is FirProperty) {
                    declaration.getter?.replaceReturnTypeRef(returnTypeRef)
                    declaration.setter?.valueParameters?.firstOrNull()?.replaceReturnTypeRef(returnTypeRef)
                }

                declaration.attributes.deferredCallableCopyReturnType = null

                return returnTypeRef
            }
        }

        protected abstract fun FirCallableDeclaration.getResolvedTypeRef(): FirResolvedTypeRef?
    }

    /**
     * See [DeferredCallableCopyTypeCalculator].
     */
    object Forced : DeferredCallableCopyTypeCalculator() {
        override fun FirCallableDeclaration.getResolvedTypeRef(): FirResolvedTypeRef? {
            return returnTypeRef as? FirResolvedTypeRef
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------------

private object DeferredCallableCopyReturnTypeKey : FirDeclarationDataKey()

var FirDeclarationAttributes.deferredCallableCopyReturnType: DeferredCallableCopyReturnType? by FirDeclarationDataRegistry.attributesAccessor(
    DeferredCallableCopyReturnTypeKey
)

abstract class DeferredCallableCopyReturnType {
    /**
     * Performs a deferred computation some declaration's return type.
     *
     * [calc] must be used for the return type calculation of overridden members which might recursively trigger the computation of
     * deferred return types.
     */
    abstract fun computeReturnType(calc: CallableCopyTypeCalculator): ConeKotlinType?
}

