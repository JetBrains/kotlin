/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.diagnostics.ConeIntermediateDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeSimpleDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeUnexpectedTypeArgumentsError
import org.jetbrains.kotlin.fir.diagnostics.DiagnosticKind
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedQualifierError
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeWrongNumberOfTypeArgumentsError
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.name.ClassId

@ThreadSafeMutableState
class FirTypeResolverImpl(private val session: FirSession) : FirTypeResolver() {

    private val symbolProvider by lazy {
        session.symbolProvider
    }

    private data class ClassIdInSession(val session: FirSession, val id: ClassId)

    private val implicitBuiltinTypeSymbols = mutableMapOf<ClassIdInSession, FirClassLikeSymbol<*>>()

    // TODO: get rid of session used here, and may be also of the cache above (see KT-30275)
    private fun resolveBuiltInQualified(id: ClassId, session: FirSession): FirClassLikeSymbol<*> {
        val nameInSession = ClassIdInSession(session, id)
        return implicitBuiltinTypeSymbols.getOrPut(nameInSession) {
            symbolProvider.getClassLikeSymbolByFqName(id)!!
        }
    }

    private fun resolveToSymbol(
        typeRef: FirTypeRef,
        scope: FirScope
    ): Pair<FirClassifierSymbol<*>?, ConeSubstitutor?> {
        return when (typeRef) {
            is FirResolvedTypeRef -> {
                val resultSymbol = typeRef.coneTypeSafe<ConeLookupTagBasedType>()?.lookupTag?.let(symbolProvider::getSymbolByLookupTag)
                resultSymbol to null
            }

            is FirUserTypeRef -> {
                val qualifierResolver = session.qualifierResolver
                var resolvedSymbol: FirClassifierSymbol<*>? = null
                var substitutor: ConeSubstitutor? = null
                scope.processClassifiersByNameWithSubstitution(typeRef.qualifier.first().name) { symbol, substitutorFromScope ->
                    if (resolvedSymbol != null) return@processClassifiersByNameWithSubstitution
                    resolvedSymbol = when (symbol) {
                        is FirClassLikeSymbol<*> -> {
                            if (typeRef.qualifier.size == 1) {
                                symbol
                            } else {
                                qualifierResolver.resolveSymbolWithPrefix(typeRef.qualifier, symbol.classId)
                            }
                        }
                        is FirTypeParameterSymbol -> {
                            assert(typeRef.qualifier.size == 1)
                            symbol
                        }
                        else -> error("!")
                    }
                    substitutor = substitutorFromScope
                }

                // TODO: Imports
                val resultSymbol: FirClassifierSymbol<*>? = resolvedSymbol ?: qualifierResolver.resolveSymbol(typeRef.qualifier)
                resultSymbol to substitutor
            }

            is FirImplicitBuiltinTypeRef -> {
                resolveBuiltInQualified(typeRef.id, session) to null
            }

            else -> null to null
        }
    }

    private fun resolveUserType(
        typeRef: FirUserTypeRef,
        symbol: FirClassifierSymbol<*>?,
        substitutor: ConeSubstitutor?,
        areBareTypesAllowed: Boolean
    ): ConeKotlinType {
        if (symbol == null) {
            return ConeKotlinErrorType(ConeUnresolvedQualifierError(typeRef.render()))
        }
        if (symbol is FirTypeParameterSymbol) {
            for (part in typeRef.qualifier) {
                if (part.typeArgumentList.typeArguments.isNotEmpty()) {
                    return ConeClassErrorType(
                        ConeUnexpectedTypeArgumentsError("Type arguments not allowed", part.typeArgumentList.source)
                    )
                }
            }
        }
        var typeArguments = typeRef.qualifier.toTypeProjections()
        if (symbol is FirRegularClassSymbol) {
            val isPossibleBareType = areBareTypesAllowed && typeArguments.isEmpty()
            if (typeArguments.size != symbol.fir.typeParameters.size && !isPossibleBareType) {
                @Suppress("NAME_SHADOWING")
                if (symbol.fir.typeParameters.size < typeArguments.size) {
                    return ConeClassErrorType(ConeWrongNumberOfTypeArgumentsError(symbol.fir.typeParameters.size, symbol))
                } else {
                    val substitutor = substitutor ?: ConeSubstitutor.Empty
                    val argumentsFromOuterClassesAndParents = symbol.fir.typeParameters.drop(typeArguments.size).mapNotNull {
                        val type = ConeTypeParameterTypeImpl(ConeTypeParameterLookupTag(it.symbol), isNullable = false)
                        // we should report ConeSimpleDiagnostic(..., WrongNumberOfTypeArguments)
                        // but genericArgumentNumberMismatch.kt test fails with
                        // index out of bounds exception for start offset of
                        // the source
                        substitutor.substituteOrNull(type)

                    }.toTypedArray<ConeTypeProjection>()
                    typeArguments += argumentsFromOuterClassesAndParents

                    if (typeArguments.size != symbol.fir.typeParameters.size) {
                        return ConeClassErrorType(
                            ConeWrongNumberOfTypeArgumentsError(
                                desiredCount = symbol.fir.typeParameters.size - argumentsFromOuterClassesAndParents.size,
                                type = symbol
                            )
                        )
                    }
                }
            }
        }
        return symbol.constructType(typeArguments, typeRef.isMarkedNullable, typeRef.annotations.computeTypeAttributes())
            .also {
                val lookupTag = it.lookupTag
                if (lookupTag is ConeClassLikeLookupTagImpl && symbol is FirClassLikeSymbol<*>) {
                    lookupTag.bindSymbolToLookupTag(session, symbol)
                }
            }
    }

    private fun createFunctionalType(typeRef: FirFunctionTypeRef): ConeClassLikeType {
        val parameters =
            listOfNotNull(typeRef.receiverTypeRef?.coneType) +
                    typeRef.valueParameters.map { it.returnTypeRef.coneType } +
                    listOf(typeRef.returnTypeRef.coneType)
        val classId = if (typeRef.isSuspend) {
            StandardNames.getSuspendFunctionClassId(typeRef.parametersCount)
        } else {
            StandardNames.getFunctionClassId(typeRef.parametersCount)
        }
        val attributes = typeRef.annotations.computeTypeAttributes()
        return ConeClassLikeTypeImpl(
            resolveBuiltInQualified(classId, session).toLookupTag(),
            parameters.toTypedArray(),
            typeRef.isMarkedNullable,
            attributes
        )
    }

    override fun resolveType(
        typeRef: FirTypeRef,
        scope: FirScope,
        areBareTypesAllowed: Boolean
    ): ConeKotlinType {
        return when (typeRef) {
            is FirResolvedTypeRef -> typeRef.type
            is FirUserTypeRef -> {
                val (symbol, substitutor) = resolveToSymbol(typeRef, scope)
                resolveUserType(typeRef, symbol, substitutor, areBareTypesAllowed)
            }
            is FirFunctionTypeRef -> createFunctionalType(typeRef)
            is FirDynamicTypeRef -> ConeKotlinErrorType(ConeIntermediateDiagnostic("Not supported: ${typeRef::class.simpleName}"))
            else -> error(typeRef.render())
        }
    }
}
