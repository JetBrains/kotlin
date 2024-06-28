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
        session.firCachesFactory.createCache<FirClassSymbol<*>, Set<TypeWithOrigin>, Nothing?> { symbol, _ ->
            findMissingSuperTypes(symbol)
        }

    enum class SupertypeOrigin {
        TYPE_ARGUMENT,
        OTHER
    }

    data class TypeWithOrigin(val type: ConeKotlinType, val origin: SupertypeOrigin)

    fun getMissingSuperTypes(declaration: FirClassSymbol<*>): Set<TypeWithOrigin> {
        return cache.getValue(declaration, null)
    }

    private fun findMissingSuperTypes(declaration: FirClassSymbol<*>): Set<TypeWithOrigin> {
        return declaration.collectSuperTypes(session)
            .filterTo(mutableSetOf()) { (type, _) ->
                // Ignore types which are already errors.
                type !is ConeErrorType && type !is ConeDynamicType && type.lowerBoundIfFlexible().let {
                    it is ConeLookupTagBasedType && it.toSymbol(session) == null
                }
            }
    }

    private fun FirClassSymbol<*>.collectSuperTypes(session: FirSession): Set<TypeWithOrigin> {
        val result = mutableSetOf<TypeWithOrigin>()
        fun collect(symbol: FirClassSymbol<*>, origin: SupertypeOrigin) {
            for (superTypeRef in symbol.resolvedSuperTypeRefs) {
                val superType = superTypeRef.type
                if (!superType.isAny && result.add(TypeWithOrigin(superType, origin))) {
                    (superType.toSymbol(session) as? FirClassSymbol<*>)?.let { collect(it, origin) }
                }
                for (typeArgument in superType.typeArguments) {
                    if (typeArgument !is ConeKotlinTypeProjection) continue
                    val type = typeArgument.type
                    if (!type.isAny && result.add(TypeWithOrigin(type, SupertypeOrigin.TYPE_ARGUMENT))) {
                        (type.toSymbol(session) as? FirClassSymbol<*>)?.let { collect(it, SupertypeOrigin.TYPE_ARGUMENT) }
                    }
                }
            }
        }
        collect(this, SupertypeOrigin.OTHER)
        return result
    }
}

val FirSession.missingDependencyStorage: FirMissingDependencyStorage by FirSession.sessionComponentAccessor()
