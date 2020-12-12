/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.resolve.getSymbolByLookupTag
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.*
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

fun FirSymbolProvider.getClassDeclaredPropertySymbols(classId: ClassId, name: Name): List<FirVariableSymbol<*>> {
    val classSymbol = getClassLikeSymbolByFqName(classId) as? FirRegularClassSymbol ?: return emptyList()
    val declaredMemberScope = declaredMemberScope(classSymbol.fir)
    val result = mutableListOf<FirVariableSymbol<*>>()
    declaredMemberScope.processPropertiesByName(name, result::add)

    return result
}

inline fun <reified T : AbstractFirBasedSymbol<*>> FirSymbolProvider.getSymbolByTypeRef(typeRef: FirTypeRef): T? {
    val lookupTag = typeRef.coneTypeSafe<ConeLookupTagBasedType>()?.lookupTag ?: return null
    return getSymbolByLookupTag(lookupTag) as? T
}
