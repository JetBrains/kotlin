/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

@ThreadSafeMutableState
class FirCorrespondingSupertypesCache(private val session: FirSession) : FirSessionComponent {
    private val cache = HashMap<ConeClassLikeLookupTag, Map<ConeClassLikeLookupTag, List<ConeClassLikeType>>?>(1000, 0.5f)

    fun getCorrespondingSupertypes(
        type: ConeKotlinType,
        supertypeConstructor: TypeConstructorMarker
    ): List<ConeClassLikeType>? {
        if (type !is ConeClassLikeType || supertypeConstructor !is ConeClassLikeLookupTag) return null

        val typeCheckerState = session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = true
        )

        val lookupTag = type.lookupTag
        if (lookupTag == supertypeConstructor) return listOf(captureType(type, typeCheckerState.typeSystemContext))
        if (lookupTag !in cache) {
            cache[lookupTag] = computeSupertypesMap(lookupTag, typeCheckerState)
        }

        val resultTypes = cache[lookupTag]?.getOrDefault(supertypeConstructor, emptyList()) ?: return null
        if (type.typeArguments.isEmpty()) return resultTypes

        val capturedType = captureType(type, typeCheckerState.typeSystemContext)
        val substitutionSupertypePolicy = typeCheckerState.substitutionSupertypePolicy(capturedType)
        return resultTypes.map {
            substitutionSupertypePolicy.transformType(typeCheckerState, it) as ConeClassLikeType
        }
    }

    private fun captureType(type: ConeClassLikeType, typeSystemContext: ConeTypeContext): ConeClassLikeType =
        (typeSystemContext.captureFromArguments(type, CaptureStatus.FOR_SUBTYPING) ?: type) as ConeClassLikeType

    private fun computeSupertypesMap(
        subtypeLookupTag: ConeClassLikeLookupTag,
        state: ConeTypeCheckerState
    ): Map<ConeClassLikeLookupTag, List<ConeClassLikeType>>? {
        val resultingMap = HashMap<ConeClassLikeLookupTag, List<ConeClassLikeType>>()

        val subtypeFirClass: FirClassLikeDeclaration = subtypeLookupTag.toSymbol(session)?.fir ?: return null

        val defaultType = subtypeLookupTag.constructClassType(
            (subtypeFirClass as? FirTypeParameterRefsOwner)?.typeParameters?.map {
                it.symbol.toLookupTag().constructType(emptyArray(), isNullable = false)
            }?.toTypedArray().orEmpty(),
            isNullable = false
        )

        if (state.anySupertype(
                defaultType,
                { it !is ConeClassLikeType || it.lookupTag.toSymbol(session) !is FirClassLikeSymbol<*> }
            ) { supertype -> computeSupertypePolicyAndPutInMap(supertype, resultingMap, state) }
        ) {
            return null
        }

        return resultingMap.also {
            it.remove(subtypeLookupTag) // Just optimization: do not preserve mapping from MyClass to MyClas itself
        }
    }

    private fun computeSupertypePolicyAndPutInMap(
        supertype: SimpleTypeMarker,
        resultingMap: MutableMap<ConeClassLikeLookupTag, List<ConeClassLikeType>>,
        state: ConeTypeCheckerState
    ): TypeCheckerState.SupertypesPolicy {
        val supertypeLookupTag = (supertype as ConeClassLikeType).lookupTag
        val captured =
            state.typeSystemContext.captureFromArguments(supertype, CaptureStatus.FOR_SUBTYPING) as ConeClassLikeType? ?: supertype

        resultingMap[supertypeLookupTag] = listOf(captured)

        return when {
            with(state.typeSystemContext) { captured.argumentsCount() } == 0 -> {
                TypeCheckerState.SupertypesPolicy.LowerIfFlexible
            }
            else -> {
                state.substitutionSupertypePolicy(captured)
            }
        }
    }
}
