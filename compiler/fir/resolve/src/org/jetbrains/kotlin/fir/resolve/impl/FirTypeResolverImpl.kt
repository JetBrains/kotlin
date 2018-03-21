/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.types.Variance

class FirTypeResolverImpl : FirTypeResolver {


    private fun List<FirQualifierPart>.toTypeProjections() = flatMap {
        it.typeArguments.map {
            when (it) {
                is FirStarProjection -> StarProjection
                is FirTypeProjectionWithVariance -> {
                    val type = (it.type as FirResolvedType).type
                    when (it.variance) {
                        Variance.INVARIANT -> type
                        Variance.IN_VARIANCE -> ConeKotlinTypeProjectionInImpl(type)
                        Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOutImpl(type)
                    }
                }
                else -> error("!")
            }
        }
    }

    private fun ConeSymbol.toConeKotlinType(parts: List<FirQualifierPart>, session: FirSession): ConeKotlinType? {
        val firProvider = FirProvider.getInstance(session)
        val fir = firProvider.getFirClassifierBySymbol(this) ?: return null
        return when (fir) {
            is FirTypeParameter -> {
                ConeTypeParameterTypeImpl(this as ConeTypeParameterSymbol)
            }
            is FirClass -> {
                ConeClassTypeImpl(this as ConeClassLikeSymbol, parts.toTypeProjections())
            }
            is FirTypeAlias -> {
                val coneTypeSafe = fir.expandedType.coneTypeSafe<ConeClassLikeType>()
                val expansion = if (coneTypeSafe != null) coneTypeSafe else {
                    return ConeKotlinErrorType("Couldn't resolve expansion")
                }

                ConeAbbreviatedTypeImpl(
                    abbreviationSymbol = this as ConeClassLikeSymbol,
                    typeArguments = parts.toTypeProjections(),
                    directExpansion = expansion
                )
            }
            else -> error("!")
        }
    }

    override fun resolveToSymbol(type: FirType, scope: FirScope): ConeSymbol? {
        return when (type) {
            is FirResolvedType -> type.coneTypeSafe<ConeSymbolBasedType>()?.symbol
            is FirUserType -> {

                val qualifierResolver = FirQualifierResolver.getInstance(type.session)

                var resolvedSymbol: ConeSymbol? = null
                scope.processClassifiersByName(type.qualifier.first().name) { symbol ->
                    resolvedSymbol = when (symbol) {
                        is ConeClassLikeSymbol -> {
                            qualifierResolver.resolveSymbolWithPrefix(type.qualifier, symbol.classId)
                        }
                        is ConeTypeParameterSymbol -> {
                            assert(type.qualifier.size == 1)
                            symbol
                        }
                        else -> error("!")
                    }
                    resolvedSymbol == null
                }

                // TODO: Imports
                resolvedSymbol ?: qualifierResolver.resolveSymbol(type.qualifier)
            }
            else -> null
        }
    }

    override fun resolveUserType(type: FirUserType, symbol: ConeSymbol?, scope: FirScope): ConeKotlinType {
        symbol ?: return ConeKotlinErrorType("Symbol not found")
        return symbol.toConeKotlinType(type.qualifier, type.session) ?: ConeKotlinErrorType("Failed to resolve qualified type")
    }

    override fun resolveType(type: FirType, scope: FirScope): ConeKotlinType {
        return when (type) {
            is FirResolvedType -> type.type
            is FirUserType -> {
                resolveUserType(type, resolveToSymbol(type, scope), scope)
            }
            is FirErrorType -> {
                ConeKotlinErrorType(type.reason)
            }
            is FirFunctionType, is FirDynamicType, is FirImplicitType, is FirDelegatedType -> {
                ConeKotlinErrorType("Not supported: ${type::class.simpleName}")
            }
            else -> error("!")
        }
    }
}