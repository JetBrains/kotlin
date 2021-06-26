/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry
import org.jetbrains.kotlin.fir.declarations.FirTypedDeclaration
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolvedTypeFromPrototype
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef

abstract class FakeOverrideTypeCalculator {
    abstract fun computeReturnType(declaration: FirTypedDeclaration): FirTypeRef

    object DoNothing : FakeOverrideTypeCalculator() {
        override fun computeReturnType(declaration: FirTypedDeclaration): FirTypeRef {
            return declaration.returnTypeRef
        }
    }

    object Forced : FakeOverrideTypeCalculator() {
        override fun computeReturnType(declaration: FirTypedDeclaration): FirResolvedTypeRef {
            (declaration.returnTypeRef as? FirResolvedTypeRef)?.let { return it }
            val (substitutor, baseSymbol) = declaration.attributes.fakeOverrideSubstitution ?: error("")
            val baseDeclaration = baseSymbol.fir as FirTypedDeclaration
            val baseReturnType = computeReturnType(baseDeclaration).type
            declaration.attributes.fakeOverrideSubstitution = null
            val coneType = substitutor.substituteOrSelf(baseReturnType)
            val returnType = declaration.returnTypeRef.resolvedTypeFromPrototype(coneType)
            declaration.replaceReturnTypeRef(returnType)
            return returnType
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
