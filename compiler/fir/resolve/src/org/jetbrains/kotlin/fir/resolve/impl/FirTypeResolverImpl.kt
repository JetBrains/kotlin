/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.resolve.constructType
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.*
import org.jetbrains.kotlin.name.ClassId

class FirTypeResolverImpl(private val session: FirSession) : FirTypeResolver {

    private val symbolProvider by lazy {
        session.getService(FirSymbolProvider::class)
    }

    private data class ClassIdInSession(val session: FirSession, val id: ClassId)

    private val implicitBuiltinTypeSymbols = mutableMapOf<ClassIdInSession, ConeClassLikeSymbol>()

    // TODO: get rid of session used here, and may be also of the cache above (see KT-30275)
    private fun resolveBuiltInQualified(id: ClassId, session: FirSession): ConeClassLikeSymbol {
        val nameInSession = ClassIdInSession(session, id)
        return implicitBuiltinTypeSymbols.getOrPut(nameInSession) {
            symbolProvider.getClassLikeSymbolByFqName(id) as ConeClassLikeSymbol
        }
    }

    override fun resolveToSymbol(
        typeRef: FirTypeRef,
        scope: FirScope,
        position: FirPosition
    ): ConeClassifierSymbol? {
        return when (typeRef) {
            is FirResolvedTypeRef -> typeRef.coneTypeSafe<ConeLookupTagBasedType>()?.lookupTag?.let(symbolProvider::getSymbolByLookupTag)
            is FirUserTypeRef -> {

                val qualifierResolver = FirQualifierResolver.getInstance(session)

                var resolvedSymbol: ConeClassifierSymbol? = null
                scope.processClassifiersByName(typeRef.qualifier.first().name, position) { symbol ->
                    resolvedSymbol = when (symbol) {
                        is ConeClassLikeSymbol -> {
                            if (typeRef.qualifier.size == 1) {
                                symbol
                            } else {
                                qualifierResolver.resolveSymbolWithPrefix(typeRef.qualifier, symbol.classId)
                            }
                        }
                        is ConeTypeParameterSymbol -> {
                            assert(typeRef.qualifier.size == 1)
                            symbol
                        }
                        else -> error("!")
                    }
                    resolvedSymbol == null
                }

                // TODO: Imports
                resolvedSymbol ?: qualifierResolver.resolveSymbol(typeRef.qualifier)
            }
            is FirImplicitBuiltinTypeRef -> {
                resolveBuiltInQualified(typeRef.id, session)
            }
            else -> null
        }
    }

    override fun resolveUserType(typeRef: FirUserTypeRef, symbol: ConeClassifierSymbol?, scope: FirScope): ConeKotlinType {
        symbol ?: return ConeKotlinErrorType("Symbol not found, for `${typeRef.render()}`")
        return symbol.constructType(typeRef.qualifier, typeRef.isMarkedNullable)
    }

    override fun resolveType(
        typeRef: FirTypeRef,
        scope: FirScope,
        position: FirPosition
    ): ConeKotlinType {
        return when (typeRef) {
            is FirResolvedTypeRef -> typeRef.type
            is FirUserTypeRef -> {
                resolveUserType(typeRef, resolveToSymbol(typeRef, scope, position), scope)
            }
            is FirErrorTypeRef -> {
                ConeKotlinErrorType(typeRef.reason)
            }
            is FirFunctionTypeRef -> {
                ConeFunctionTypeImpl(
                    (typeRef.receiverTypeRef as FirResolvedTypeRef?)?.type,
                    typeRef.valueParameters.map { it.returnTypeRef.coneTypeUnsafe() },
                    typeRef.returnTypeRef.coneTypeUnsafe(),
                    resolveBuiltInQualified(KotlinBuiltIns.getFunctionClassId(typeRef.parametersCount), session).toLookupTag(),
                    typeRef.isMarkedNullable
                )
            }
            is FirImplicitBuiltinTypeRef -> {
                resolveToSymbol(typeRef, scope, position)!!.constructType(emptyList(), isNullable = false)
            }
            is FirDynamicTypeRef, is FirImplicitTypeRef, is FirDelegatedTypeRef -> {
                ConeKotlinErrorType("Not supported: ${typeRef::class.simpleName}")
            }
            else -> error("!")
        }
    }
}
