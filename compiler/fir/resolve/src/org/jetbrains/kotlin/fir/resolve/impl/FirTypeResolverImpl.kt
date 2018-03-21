/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl

class FirTypeResolverImpl : FirTypeResolver {
    override fun resolveType(type: FirType, scope: FirScope): ConeKotlinType {
        return when (type) {
            is FirResolvedType -> type.type
            is FirUserType -> {

                val qualifierResolver = FirQualifierResolver.getInstance(type.session)

                var resolvedType: ConeKotlinType? = null
                scope.processClassifiersByName(type.qualifier.first().name) { symbol ->
                    resolvedType = when (symbol) {
                        is ConeClassLikeSymbol -> {
                            qualifierResolver.resolveTypeWithPrefix(type.qualifier, symbol.classId)
                        }
                        is ConeTypeParameterSymbol -> {
                            assert(type.qualifier.size == 1)
                            ConeTypeParameterTypeImpl(symbol)
                        }
                        else -> error("!")
                    }
                    resolvedType == null
                }

                // TODO: Imports
                resolvedType ?: qualifierResolver.resolveType(type.qualifier) ?: ConeKotlinErrorType("Failed to resolve qualified type")
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