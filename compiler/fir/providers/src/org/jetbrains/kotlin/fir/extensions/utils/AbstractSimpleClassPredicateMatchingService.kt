/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions.utils

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol

/**
 * Utility base class for services which may quickly answer to question
 *   "is this class or some of its superclasses matches specific predicate or not?"
 */
abstract class AbstractSimpleClassPredicateMatchingService(session: FirSession) : FirExtensionSessionComponent(session) {
    protected abstract val predicate: DeclarationPredicate

    final override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    fun isAnnotated(symbol: FirRegularClassSymbol): Boolean {
        return cache.getValue(symbol)
    }

    private val cache: FirCache<FirRegularClassSymbol, Boolean, Nothing?> = session.firCachesFactory.createCache { symbol, _ ->
        symbol.annotated()
    }

    private fun FirRegularClassSymbol.annotated(): Boolean {
        if (session.predicateBasedProvider.matches(predicate, this)) return true
        return resolvedSuperTypes.any {
            val superSymbol = it.toRegularClassSymbol(session) ?: return@any false
            cache.getValue(superSymbol)
        }
    }
}
