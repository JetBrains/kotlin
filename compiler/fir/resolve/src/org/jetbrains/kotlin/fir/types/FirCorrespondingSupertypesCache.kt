/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.resolve.constructClassType
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.types.AbstractTypeCheckerContext
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.model.SimpleTypeMarker
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

class FirCorrespondingSupertypesCache(private val session: FirSession) : FirSessionComponent {
    private val context = ConeTypeCheckerContext(isErrorTypeEqualsToAnything = false, isStubTypeEqualsToAnything = true, session = session)
    private val cache = HashMap<FirClassLikeSymbol<*>, Map<FirClassLikeSymbol<*>, List<ConeClassLikeType>>?>(1000, 0.5f)

    fun getCorrespondingSupertypes(
        type: ConeKotlinType,
        supertypeConstructor: TypeConstructorMarker
    ): List<ConeClassLikeType>? {
        if (type !is ConeClassLikeType || supertypeConstructor !is FirClassLikeSymbol<*>) return null

        val symbol = type.lookupTag.toSymbol(session) ?: return null
        if (symbol == supertypeConstructor) return listOf(captureType(type))

        if (symbol !in cache) {
            cache[symbol] = computeSupertypesMap(type, symbol)
        }

        val resultTypes = cache[symbol]?.getOrDefault(supertypeConstructor, emptyList()) ?: return null
        if (type.typeArguments.isEmpty()) return resultTypes

        val capturedType = captureType(type)
        val substitutionSupertypePolicy = context.substitutionSupertypePolicy(capturedType)
        return resultTypes.map {
            substitutionSupertypePolicy.transformType(context, it) as ConeClassLikeType
        }
    }

    private fun captureType(type: ConeClassLikeType): ConeClassLikeType =
        (context.captureFromArguments(type, CaptureStatus.FOR_SUBTYPING) ?: type) as ConeClassLikeType

    private fun computeSupertypesMap(
        subtype: ConeLookupTagBasedType,
        subtypeSymbol: FirClassLikeSymbol<*>
    ): Map<FirClassLikeSymbol<*>, List<ConeClassLikeType>>? {
        val resultingMap = HashMap<FirClassLikeSymbol<*>, List<ConeClassLikeType>>()

        val subtypeClassSymbol: FirClassLikeSymbol<*> = with(context) {
            subtype.typeConstructor() as? FirClassLikeSymbol<*> ?: return null
        }
        val subtypeFirClass: FirClassLikeDeclaration<*> = subtypeClassSymbol.fir

        val defaultType = subtypeClassSymbol.toLookupTag().constructClassType(
            (subtypeFirClass as? FirTypeParametersOwner)?.typeParameters?.map {
                it.symbol.toLookupTag().constructType(emptyArray(), isNullable = false)
            }?.toTypedArray().orEmpty(),
            isNullable = false
        )

        if (context.anySupertype(
                defaultType,
                { it !is ConeClassLikeType || it.lookupTag.toSymbol(session) !is FirClassLikeSymbol<*> }
            ) { supertype -> computeSupertypePolicyAndPutInMap(supertype, subtypeSymbol, resultingMap) }
        ) {
            return null
        }

        return resultingMap
    }

    private fun computeSupertypePolicyAndPutInMap(
        supertype: SimpleTypeMarker,
        subtypeSymbol: FirClassLikeSymbol<*>,
        resultingMap: MutableMap<FirClassLikeSymbol<*>, List<ConeClassLikeType>>
    ): AbstractTypeCheckerContext.SupertypesPolicy {
        val supertypeSymbol = (supertype as ConeClassLikeType).lookupTag.toSymbol(session) as FirClassLikeSymbol<*>
        val captured = context.captureFromArguments(supertype, CaptureStatus.FOR_SUBTYPING) as ConeClassLikeType? ?: supertype

        if (supertypeSymbol != subtypeSymbol) {
            resultingMap[supertypeSymbol] = listOf(captured)
        }

        return when {
            with(context) { captured.argumentsCount() } == 0 -> {
                AbstractTypeCheckerContext.SupertypesPolicy.LowerIfFlexible
            }
            else -> {
                context.substitutionSupertypePolicy(captured)
            }
        }
    }
}
