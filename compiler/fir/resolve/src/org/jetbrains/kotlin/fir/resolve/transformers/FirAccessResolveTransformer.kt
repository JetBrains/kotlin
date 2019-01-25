/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirNamedReference
import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedCallableReferenceImpl
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.impl.FirClassDeclaredMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTopLevelDeclaredMemberScope
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

class FirAccessResolveTransformer : FirAbstractTreeTransformerWithSuperTypes(reversedScopePriority = true) {

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        return withScopeCleanup {
            towerScope.scopes += FirTopLevelDeclaredMemberScope(file, file.session)
            super.transformFile(file, data)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            lookupSuperTypes(regularClass).asReversed().mapNotNullTo(towerScope.scopes) {
                val symbol = it.symbol
                if (symbol is FirClassSymbol) {
                    FirClassDeclaredMemberScope(symbol.fir, symbol.fir.session)
                } else {
                    null
                }
            }
            towerScope.scopes += FirClassDeclaredMemberScope(regularClass, regularClass.session)
            super.transformRegularClass(regularClass, data)
        }
    }

    override fun transformNamedReference(namedReference: FirNamedReference, data: Nothing?): CompositeTransformResult<FirNamedReference> {
        if (namedReference is FirResolvedCallableReference) return namedReference.compose()
        val name = namedReference.name
        var symbol: ConeCallableSymbol? = null
        towerScope.processClassifiersByName(name, FirPosition.OTHER) {
            symbol = it as? ConeCallableSymbol
            // I'm lucky today!!! (the first symbol we have found is the one we need)
            it is ConeCallableSymbol
        }
        val callableSymbol = symbol ?: return FirErrorNamedReference(
            namedReference.session, namedReference.psi, "Unresolved name: $name"
        ).compose()
        return FirResolvedCallableReferenceImpl(
            namedReference.session, namedReference.psi,
            name, callableSymbol
        ).compose()
    }
}