/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.toFirClassLike
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

abstract class FirSymbolProvider : FirSessionComponent {

    abstract fun getClassLikeSymbolByFqName(classId: ClassId): FirClassLikeSymbol<*>?

    fun getSymbolByLookupTag(lookupTag: ConeClassifierLookupTag): ConeClassifierSymbol? {
        return when (lookupTag) {
            is ConeClassLikeLookupTag -> {
                (lookupTag as? ConeClassLikeLookupTagImpl)
                    ?.boundSymbol?.takeIf { it.first === this }?.let { return it.second }

                getClassLikeSymbolByFqName(lookupTag.classId).also {
                    (lookupTag as? ConeClassLikeLookupTagImpl)
                        ?.boundSymbol = Pair(this, it)
                }
            }
            is ConeClassifierLookupTagWithFixedSymbol -> lookupTag.symbol
            else -> error("Unknown lookupTag type: ${lookupTag::class}")
        }
    }

    abstract fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<FirCallableSymbol<*>>

    abstract fun getClassDeclaredMemberScope(classId: ClassId): FirScope?
    abstract fun getClassUseSiteMemberScope(
        classId: ClassId,
        useSiteSession: FirSession,
        scopeSession: ScopeSession
    ): FirScope?

    open fun getAllCallableNamesInPackage(fqName: FqName): Set<Name> = emptySet()
    open fun getClassNamesInPackage(fqName: FqName): Set<Name> = emptySet()

    open fun getAllCallableNamesInClass(classId: ClassId): Set<Name> = emptySet()
    open fun getNestedClassesNamesInClass(classId: ClassId): Set<Name> = emptySet()

    abstract fun getPackage(fqName: FqName): FqName? // TODO: Replace to symbol sometime

    // TODO: should not retrieve session through the FirElement::session
    fun getSessionForClass(classId: ClassId): FirSession? = getClassLikeSymbolByFqName(classId)?.toFirClassLike()?.session

    companion object {
        fun getInstance(session: FirSession) = session.service<FirSymbolProvider>()
    }
}

fun FirSymbolProvider.getClassDeclaredCallableSymbols(classId: ClassId, name: Name): List<ConeCallableSymbol> {
    val declaredMemberScope = getClassDeclaredMemberScope(classId) ?: return emptyList()
    val result = mutableListOf<ConeCallableSymbol>()
    val processor: (ConeCallableSymbol) -> ProcessorAction = {
        result.add(it)
        ProcessorAction.NEXT
    }

    declaredMemberScope.processFunctionsByName(name, processor)
    declaredMemberScope.processPropertiesByName(name, processor)

    return result
}

