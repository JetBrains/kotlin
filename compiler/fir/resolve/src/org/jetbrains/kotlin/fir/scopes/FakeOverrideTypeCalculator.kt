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
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

abstract class FakeOverrideTypeCalculator {
    abstract fun computeReturnType(declaration: FirTypedDeclaration): ConeKotlinType?

    class DoNothing : FakeOverrideTypeCalculator() {
        override fun computeReturnType(declaration: FirTypedDeclaration): ConeKotlinType? {
            return declaration.returnTypeRef.coneTypeSafe()
        }
    }
}

class ForcedFakeOverrideTypeCalculator : FakeOverrideTypeCalculator() {
    override fun computeReturnType(declaration: FirTypedDeclaration): ConeKotlinType {
        declaration.returnTypeRef.coneTypeSafe<ConeKotlinType>()?.let { return it }
        val (substitutor, baseSymbol) = declaration.attributes.fakeOverrideSubstitution ?: error("")
        val baseDeclaration = baseSymbol.fir as FirTypedDeclaration
        val returnType = computeReturnType(baseDeclaration)
        declaration.attributes.fakeOverrideSubstitution = null
        return substitutor.substituteOrSelf(returnType)
    }
}

// ---------------------------------------------------------------------------------------------------------------------------------------

object FakeOverrideSubstitutionKey : FirDeclarationDataKey()

var FirDeclarationAttributes.fakeOverrideSubstitution: FakeOverrideSubstitution? by FirDeclarationDataRegistry.attributesAccessor(
    FakeOverrideSubstitutionKey
)

data class FakeOverrideSubstitution(
    val substitutor: ConeSubstitutor,
    val baseSymbol: AbstractFirBasedSymbol<*>
)
