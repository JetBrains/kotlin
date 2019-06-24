/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*

fun lookupSuperTypes(
    klass: FirRegularClass,
    lookupInterfaces: Boolean,
    deep: Boolean,
    useSiteSession: FirSession
): List<ConeClassLikeType> {
    return mutableListOf<ConeClassLikeType>().also {
        if (lookupInterfaces) klass.symbol.collectSuperTypes(it, deep, useSiteSession)
        else klass.symbol.collectSuperClasses(it, useSiteSession)
    }
}

class ScopeSession {
    private val scopes = mutableMapOf<ConeClassifierSymbol, MutableMap<ScopeSessionKey<*>, FirScope>>()
    fun <T : FirScope> getOrBuild(symbol: ConeClassifierSymbol, key: ScopeSessionKey<T>, build: () -> T): T {
        return scopes.getOrPut(symbol) {
            mutableMapOf()
        }.getOrPut(key) {
            build()
        } as T
    }
}

abstract class ScopeSessionKey<T : FirScope>()

inline fun <reified T : FirScope> scopeSessionKey(): ScopeSessionKey<T> {
    return object : ScopeSessionKey<T>() {}
}

val USE_SITE = scopeSessionKey<FirScope>()
val DECLARED = scopeSessionKey<FirScope>()

data class SubstitutionScopeKey<T : FirClassSubstitutionScope>(val type: ConeClassLikeType) : ScopeSessionKey<T>() {}

fun FirRegularClass.buildUseSiteScope(useSiteSession: FirSession, builder: ScopeSession): FirScope? {
    val symbolProvider = useSiteSession.service<FirSymbolProvider>()
    return symbolProvider.getClassUseSiteMemberScope(this.classId, useSiteSession, builder)
}

fun FirTypeAlias.buildUseSiteScope(useSiteSession: FirSession, builder: ScopeSession): FirScope? {
    val type = expandedTypeRef.coneTypeUnsafe<ConeClassLikeType>()
    return type.scope(useSiteSession, builder)?.let {
        type.wrapSubstitutionScopeIfNeed(useSiteSession, it, this, builder)
    }
}

fun FirRegularClass.buildDefaultUseSiteScope(useSiteSession: FirSession, builder: ScopeSession): FirScope {
    return builder.getOrBuild(symbol, USE_SITE) {

        val declaredScope = builder.getOrBuild(this.symbol, DECLARED) { declaredMemberScope(this) }
        val scopes = lookupSuperTypes(this, lookupInterfaces = true, deep = false, useSiteSession = useSiteSession)
            .mapNotNull { useSiteSuperType ->
                if (useSiteSuperType is ConeClassErrorType) return@mapNotNull null
                val symbol = useSiteSuperType.lookupTag.toSymbol(useSiteSession)
                if (symbol is FirClassSymbol) {
                    val useSiteScope = symbol.fir.buildUseSiteScope(useSiteSession, builder)!!
                    useSiteSuperType.wrapSubstitutionScopeIfNeed(useSiteSession, useSiteScope, symbol.fir, builder)
                } else {
                    null
                }
            }
        FirClassUseSiteScope(useSiteSession, FirSuperTypeScope(useSiteSession, scopes), declaredScope)
    }
}

private fun ConeClassLikeType.wrapSubstitutionScopeIfNeed(
    session: FirSession,
    useSiteScope: FirScope,
    declaration: FirClassLikeDeclaration<*>,
    builder: ScopeSession
): FirScope {
    if (this.typeArguments.isEmpty()) return useSiteScope
    return builder.getOrBuild(declaration.symbol, SubstitutionScopeKey(this)) {
        @Suppress("UNCHECKED_CAST")
        val substitution = declaration.typeParameters.zip(this.typeArguments) { typeParameter, typeArgument ->
            typeParameter.symbol to (typeArgument as? ConeTypedProjection)?.type
        }.filter { (_, type) -> type != null }.toMap() as Map<ConeTypeParameterSymbol, ConeKotlinType>

        FirClassSubstitutionScope(session, useSiteScope, builder, substitution)
    }
}

private tailrec fun ConeClassLikeType.computePartialExpansion(useSiteSession: FirSession): ConeClassLikeType? {
    return when (this) {
        is ConeAbbreviatedType -> directExpansionType(useSiteSession)?.computePartialExpansion(useSiteSession)
        else -> this
    }
}

private tailrec fun ConeClassifierSymbol.collectSuperClasses(
    list: MutableList<ConeClassLikeType>,
    useSiteSession: FirSession
) {
    when (this) {
        is FirClassSymbol -> {
            val superClassType =
                fir.superConeTypes
                    .map { it.computePartialExpansion(useSiteSession) }
                    .firstOrNull {
                        it !is ConeClassErrorType &&
                                (it?.lookupTag?.toSymbol(useSiteSession) as? FirClassSymbol)?.fir?.classKind == ClassKind.CLASS
                    } ?: return
            list += superClassType
            superClassType.lookupTag.toSymbol(useSiteSession)?.collectSuperClasses(list, useSiteSession)
        }
        is FirTypeAliasSymbol -> {
            val expansion = fir.expandedConeType?.computePartialExpansion(useSiteSession) ?: return
            expansion.lookupTag.toSymbol(useSiteSession)?.collectSuperClasses(list, useSiteSession)
        }
        else -> error("?!id:1")
    }
}

private fun ConeClassifierSymbol.collectSuperTypes(
    list: MutableList<ConeClassLikeType>,
    deep: Boolean,
    useSiteSession: FirSession
) {
    when (this) {
        is FirClassSymbol -> {
            val superClassTypes =
                fir.superConeTypes.mapNotNull { it.computePartialExpansion(useSiteSession) }
            list += superClassTypes
            if (deep)
                superClassTypes.forEach {
                    if (it !is ConeClassErrorType) {
                        it.lookupTag.toSymbol(useSiteSession)?.collectSuperTypes(list, deep, useSiteSession)
                    }
                }
        }
        is FirTypeAliasSymbol -> {
            val expansion = fir.expandedConeType?.computePartialExpansion(useSiteSession) ?: return
            expansion.lookupTag.toSymbol(useSiteSession)?.collectSuperTypes(list, deep, useSiteSession)
        }
        else -> error("?!id:1")
    }
}
