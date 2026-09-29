/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.declarationGenerators
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.name.Name

// TODO: we could get rid of this scope and use FirNestedClassifierScope instead,
// but in this case we should make JavaSymbolProvider greedy related to nested classifiers
// (or make possible to calculate nested classifiers on-the-fly)
class FirLazyNestedClassifierScope(
    klass: FirClass,
    useSiteSession: FirSession,
    private val existingNames: List<Name>,
) : FirNestedClassifierScope(klass, useSiteSession) {
    override fun getNestedClassSymbol(name: Name): FirClassLikeSymbol<*>? {
        var result: FirClassLikeSymbol<*>? = null
        processClassifiersByNameWithSubstitution(name) { it, _ ->
            if (it is FirClassLikeSymbol) {
                result = it
            }
        }
        return result
    }

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        val context by lazy { NestedClassGenerationContext(klass.symbol, this) }
        if (name !in existingNames &&
            useSiteSession.extensionService.declarationGenerators.none {
                name in it.getNestedClassifiersNames(klass.symbol, context)
            }
        ) {
            return
        }
        val child = klass.symbol.classId.createNestedClassId(name)
        val symbol = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(child) ?: return

        processor(symbol, ConeSubstitutor.Empty)
    }

    override fun isEmpty(): Boolean {
        return false
    }

    override fun getClassifierNames(): Set<Name> = existingNames.toSet()

    override fun getCallableNames(): Set<Name> = emptySet()

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirLazyNestedClassifierScope? {
        return FirLazyNestedClassifierScope(klass, newSession, existingNames)
    }
}
