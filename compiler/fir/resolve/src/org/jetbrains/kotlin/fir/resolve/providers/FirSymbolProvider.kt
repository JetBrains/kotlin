/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTagWithFixedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeLookupTagBasedType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

@RequiresOptIn
annotation class FirSymbolProviderInternals

abstract class FirSymbolProvider(val session: FirSession) : FirSessionComponent {
    abstract fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>?

    @OptIn(ExperimentalStdlibApi::class, FirSymbolProviderInternals::class)
    open fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>> {
        return buildList { getTopLevelCallableSymbolsTo(this, packageFqName, name) }
    }

    @FirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name)

    abstract fun getPackage(fqName: FqName): FqName? // TODO: Replace to symbol sometime
}

fun FirSymbolProvider.getClassDeclaredCallableSymbols(classId: ClassId, name: Name): List<FirCallableSymbol<*>> {
    val classSymbol = getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol ?: return emptyList()
    val declaredMemberScope = declaredMemberScope(classSymbol.fir)
    val result = mutableListOf<FirCallableSymbol<*>>()
    declaredMemberScope.processFunctionsByName(name, result::add)
    declaredMemberScope.processPropertiesByName(name, result::add)
    if (name == classId.shortClassName) declaredMemberScope.processDeclaredConstructors(result::add)

    return result
}

inline fun <reified T : AbstractFirBasedSymbol<*>> FirSymbolProvider.getSymbolByTypeRef(typeRef: FirTypeRef): T? {
    val lookupTag = typeRef.coneTypeSafe<ConeLookupTagBasedType>()?.lookupTag ?: return null
    return getSymbolByLookupTag(lookupTag) as? T
}

@OptIn(LookupTagInternals::class)
fun ConeClassLikeLookupTagImpl.bindSymbolToLookupTag(session: FirSession, symbol: FirClassLikeSymbol<*>?) {
    boundSymbol = OneElementWeakMap(session, symbol)
}

fun FirSymbolProvider.getSymbolByLookupTag(lookupTag: ConeClassifierLookupTag): FirClassifierSymbol<*>? {
    return when (lookupTag) {
        is ConeClassLikeLookupTag -> getSymbolByLookupTag(lookupTag)
        is ConeClassifierLookupTagWithFixedSymbol -> lookupTag.symbol
        else -> error("Unknown lookupTag type: ${lookupTag::class}")
    }
}

@OptIn(LookupTagInternals::class)
fun FirSymbolProvider.getSymbolByLookupTag(lookupTag: ConeClassLikeLookupTag): FirClassLikeSymbol<*>? {
    (lookupTag as? ConeClassLikeLookupTagImpl)
        ?.boundSymbol?.takeIf { it.key === this.session }?.let { return it.value }

    return getClassLikeSymbolByFqName(lookupTag.classId)
        .also {
            (lookupTag as? ConeClassLikeLookupTagImpl)?.bindSymbolToLookupTag(session, it)
        }
}
