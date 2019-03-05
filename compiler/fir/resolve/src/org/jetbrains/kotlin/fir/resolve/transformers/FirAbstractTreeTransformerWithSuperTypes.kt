/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.expandedConeType
import org.jetbrains.kotlin.fir.declarations.superConeTypes
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.symbols.ConeClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeAbbreviatedType
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType

abstract class FirAbstractTreeTransformerWithSuperTypes(reversedScopePriority: Boolean) : FirAbstractTreeTransformer() {
    protected val towerScope = FirCompositeScope(mutableListOf(), reversedPriority = reversedScopePriority)

    protected inline fun <T> withScopeCleanup(crossinline l: () -> T): T {
        val sizeBefore = towerScope.scopes.size
        val result = l()
        val size = towerScope.scopes.size
        assert(size >= sizeBefore)
        repeat(size - sizeBefore) {
            towerScope.scopes.let { it.removeAt(it.size - 1) }
        }
        return result
    }

    protected fun lookupSuperTypes(
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

    private tailrec fun ConeClassLikeType.computePartialExpansion(): ConeClassLikeType? {
        return when (this) {
            is ConeAbbreviatedType -> directExpansion.takeIf { it !is ConeClassErrorType }?.computePartialExpansion()
            else -> return this
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
                        .map { it.computePartialExpansion() }
                        .firstOrNull {
                            it !is ConeClassErrorType &&
                                    (it?.lookupTag?.toSymbol(useSiteSession) as? FirClassSymbol)?.fir?.classKind == ClassKind.CLASS
                        } ?: return
                list += superClassType
                superClassType.lookupTag.toSymbol(useSiteSession)?.collectSuperClasses(list, useSiteSession)
            }
            is FirTypeAliasSymbol -> {
                val expansion = fir.expandedConeType?.computePartialExpansion() ?: return
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
                    fir.superConeTypes.mapNotNull { it.computePartialExpansion() }
                list += superClassTypes
                if (deep)
                    superClassTypes.forEach {
                        if (it !is ConeClassErrorType) {
                            it.lookupTag.toSymbol(useSiteSession)?.collectSuperTypes(list, deep, useSiteSession)
                        }
                    }
            }
            is FirTypeAliasSymbol -> {
                val expansion = fir.expandedConeType?.computePartialExpansion() ?: return
                expansion.lookupTag.toSymbol(useSiteSession)?.collectSuperTypes(list, deep, useSiteSession)
            }
            else -> error("?!id:1")
        }
    }
}
