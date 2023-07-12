/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

abstract class FakeOverrideTypeCalculator {
    abstract fun computeReturnType(declaration: FirCallableDeclaration): FirTypeRef?

    object DoNothing : FakeOverrideTypeCalculator() {
        override fun computeReturnType(declaration: FirCallableDeclaration): FirTypeRef {
            return declaration.returnTypeRef
        }
    }

    abstract class AbstractFakeOverrideTypeCalculator : FakeOverrideTypeCalculator() {
        override fun computeReturnType(declaration: FirCallableDeclaration): FirResolvedTypeRef? {
            val fakeOverrideSubstitution = declaration.attributes.fakeOverrideSubstitution ?: return declaration.getResolvedTypeRef()

            // TODO: drop synchronized in KT-60385
            synchronized(fakeOverrideSubstitution) {
                if (declaration.attributes.fakeOverrideSubstitution == null) {
                    return declaration.returnTypeRef as FirResolvedTypeRef
                }

                val (substitutor, baseSymbol) = fakeOverrideSubstitution
                val baseDeclaration = baseSymbol.fir as FirCallableDeclaration
                val baseReturnType = computeReturnType(baseDeclaration)?.type ?: return null
                val coneType = substitutor.substituteOrSelf(baseReturnType)
                val returnType = declaration.returnTypeRef.resolvedTypeFromPrototype(coneType)
                declaration.replaceReturnTypeRef(returnType)
                declaration.attributes.fakeOverrideSubstitution = null
                return returnType
            }
        }

        protected abstract fun FirCallableDeclaration.getResolvedTypeRef(): FirResolvedTypeRef?
    }


    object Forced : AbstractFakeOverrideTypeCalculator() {
        override fun FirCallableDeclaration.getResolvedTypeRef(): FirResolvedTypeRef? {
            return returnTypeRef as? FirResolvedTypeRef
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------------

object FakeOverrideSubstitutionKey : FirDeclarationDataKey()

var FirDeclarationAttributes.fakeOverrideSubstitution: FakeOverrideSubstitution? by FirDeclarationDataRegistry.attributesAccessor(
    FakeOverrideSubstitutionKey
)

data class FakeOverrideSubstitution(
    val substitutor: ConeSubstitutor,
    val baseSymbol: FirBasedSymbol<*>
)
