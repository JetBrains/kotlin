/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
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
            .filterTo(mutableSetOf()) { type ->
                // Ignore types which are already errors.
                type !is ConeErrorType && type !is ConeDynamicType && type.lowerBoundIfFlexible().let {
                    it is ConeLookupTagBasedType && it.toSymbol(session) == null
                }
            }
    }

    private fun FirClassSymbol<*>.collectSuperTypes(session: FirSession): Set<ConeKotlinType> {
        val result = mutableSetOf<ConeKotlinType>()
        fun collect(symbol: FirClassSymbol<*>) {
            for (superTypeRef in symbol.resolvedSuperTypeRefs) {
                val superType = superTypeRef.coneType
                if (!superType.isAny && result.add(superType)) {
                    superType.toClassSymbol(session)?.let { collect(it) }
                }
            }
        }
        collect(this)
        return result
    }
}

val FirSession.missingDependencyStorage: FirMissingDependencyStorage by FirSession.sessionComponentAccessor()
