/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

@ThreadSafeMutableState
class FirMissingDependencyStorage(private val session: FirSession) : FirSessionComponent {
    private val cache =
        session.firCachesFactory.createCache<FirClassSymbol<*>, Set<ConeKotlinType>, Nothing?> { symbol, _ ->
            findMissingSuperTypes(symbol)
        }

    fun getMissingSuperTypes(declaration: FirClassSymbol<*>): Set<ConeKotlinType> {
        return cache.getValue(declaration, null)
    }

    private fun findMissingSuperTypes(declaration: FirClassSymbol<*>): Set<ConeKotlinType> {
        return declaration.collectSuperTypes(session)
            .filterTo(mutableSetOf()) {
                // Ignore types which are already errors.
                it !is ConeErrorType && it !is ConeDynamicType && it.toSymbol(session) == null
            }
    }

    private fun FirClassSymbol<*>.collectSuperTypes(session: FirSession): Set<ConeKotlinType> {
        val superTypes = mutableSetOf<ConeKotlinType>()
        fun collect(symbol: FirClassSymbol<*>) {
            for (superTypeRef in symbol.resolvedSuperTypeRefs) {
                val superType = superTypeRef.type
                if (!superType.isAny && superTypes.add(superType)) {
                    (superType.toSymbol(session) as? FirClassSymbol<*>)?.let(::collect)
                }
            }
        }
        collect(this)
        return superTypes
    }
}

val FirSession.missingDependencyStorage: FirMissingDependencyStorage by FirSession.sessionComponentAccessor()
