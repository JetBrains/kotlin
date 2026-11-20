/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.model.RigidTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

/**
 * A per-session cache of the *corresponding supertypes* of a class.
 *
 * For a given class, a corresponding supertype is a concrete instantiation of any supertype reachable through the class's inheritance
 * hierarchy. The supertype is instantiated from the class's default type arguments, to be substituted in a separate step.
 *
 * For each class, the cache maps from each superclass's lookup tag to the corresponding supertype(s).
 *
 * Computing a corresponding supertype requires walking the supertype graph and substituting type arguments through the whole inheritance
 * chain (e.g. `ArrayList<String>` has `Collection<String>` as its corresponding `Collection` supertype). This is expensive and happens very
 * frequently during subtyping checks, so the graph walk is cached once per class (keyed by [ConeClassLikeLookupTag], computed on the
 * class's default type). [getCorrespondingSupertypes] then only substitutes the queried type's actual arguments onto the cached result.
 *
 * #### Example
 *
 * Take the following classes:
 *
 * ```kotlin
 * class B<S>
 * class D<T>
 * class C<U> : D<U>
 * class A<X> : B<X>, C<List<X>>
 * ```
 *
 * The cache maps `A`'s lookup tag to the corresponding supertypes computed from `A`'s default type `A<X>`:
 *
 * - `B` -> `B<X>`
 * - `C` -> `C<List<X>>`
 * - `D` -> `D<List<X>>`
 *
 * A query for the corresponding `D` supertype of `A<String>` then reuses the cached `D<List<X>>` and substitutes `X = String` to yield
 * `D<List<String>>`.
 */
@ThreadSafeMutableState
class FirCorrespondingSupertypesCache(private val session: FirSession) : FirSessionComponent {
    private val cache =
        session.firCachesFactory.createCache<ConeClassLikeLookupTag, Map<ConeClassLikeLookupTag, List<ConeClassLikeType>>?, TypeCheckerState>(
            initialCapacity = 1000,
            loadFactor = 0.5f
        ) { lookupTag, typeCheckerState ->
            computeSupertypesMap(lookupTag, typeCheckerState)
        }

    fun getCorrespondingSupertypes(
        type: ConeKotlinType,
        supertypeConstructor: TypeConstructorMarker
    ): List<ConeClassLikeType>? {
        if (type !is ConeClassLikeType || supertypeConstructor !is ConeClassLikeLookupTag) return null

        val typeContext = session.typeContext
        val typeCheckerState = typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = true
        )

        val lookupTag = type.lookupTag
        if (lookupTag == supertypeConstructor) return listOf(captureType(type, typeContext))

        val resultTypes =
            cache.getValue(lookupTag, typeCheckerState)?.getOrDefault(supertypeConstructor, emptyList()) ?: return null
        if (type.typeArguments.isEmpty()) return resultTypes

        val capturedType = captureType(type, typeContext)
        val substitutionSupertypePolicy = typeContext.substitutionSupertypePolicy(capturedType)
        return resultTypes.map {
            substitutionSupertypePolicy.transformType(typeCheckerState, it) as ConeClassLikeType
        }
    }

    private fun captureType(type: ConeClassLikeType, typeSystemContext: ConeTypeContext): ConeClassLikeType =
        (typeSystemContext.captureFromArguments(type, CaptureStatus.FOR_SUBTYPING) ?: type) as ConeClassLikeType

    private fun computeSupertypesMap(
        subtypeLookupTag: ConeClassLikeLookupTag,
        state: TypeCheckerState
    ): Map<ConeClassLikeLookupTag, MutableList<ConeClassLikeType>>? {
        val resultingMap = HashMap<ConeClassLikeLookupTag, MutableList<ConeClassLikeType>>()

        val subtypeFirClass: FirClassLikeDeclaration = subtypeLookupTag.toSymbol(session)?.fir ?: return null

        val defaultType = subtypeLookupTag.constructClassType(
            (subtypeFirClass as? FirTypeParameterRefsOwner)?.typeParameters?.map {
                it.symbol.toLookupTag().constructType()
            }?.toTypedArray().orEmpty(),
        )

        if (state.anySupertype(
                defaultType,
                { it !is ConeClassLikeType || it.lookupTag.toSymbol(session) !is FirClassLikeSymbol<*> }
            ) { supertype -> computeSupertypePolicyAndPutInMap(supertype, resultingMap, state) }
        ) {
            return null
        }

        return resultingMap.also {
            it.remove(subtypeLookupTag) // Just optimization: do not preserve mapping from MyClass to MyClass itself
        }
    }

    private fun computeSupertypePolicyAndPutInMap(
        supertype: RigidTypeMarker,
        resultingMap: MutableMap<ConeClassLikeLookupTag, MutableList<ConeClassLikeType>>,
        state: TypeCheckerState
    ): TypeCheckerState.SupertypesPolicy {
        val supertypeLookupTag = (supertype as ConeClassLikeType).lookupTag
        val captured =
            state.typeSystemContext.captureFromArguments(supertype, CaptureStatus.FOR_SUBTYPING) as ConeClassLikeType? ?: supertype

        val list = resultingMap.computeIfAbsent(supertypeLookupTag) { mutableListOf() }
        list += captured

        return when {
            with(state.typeSystemContext) { captured.argumentsCount() } == 0 -> {
                TypeCheckerState.SupertypesPolicy.LowerIfFlexible
            }
            else -> {
                state.typeSystemContext.substitutionSupertypePolicy(captured)
            }
        }
    }
}

val FirSession.correspondingSupertypesCache: FirCorrespondingSupertypesCache by FirSession.sessionComponentAccessor()
