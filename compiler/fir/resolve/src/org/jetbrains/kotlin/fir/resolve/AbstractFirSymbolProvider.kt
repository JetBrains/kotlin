/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class AbstractFirSymbolProvider : FirSymbolProvider() {
    protected val classCache = HashMap<ClassId, ConeClassLikeSymbol?>()
    protected val topLevelCallableCache = HashMap<CallableId, List<ConeCallableSymbol>>()
    protected val packageCache = HashMap<FqName, FqName?>()

    protected inline fun <K, V : Any?> MutableMap<K, V>.lookupCacheOrCalculate(key: K, crossinline l: (K) -> V): V? {
        return if (containsKey(key)) {
            this[key]
        } else {
            val calculated = l(key)
            this[key] = calculated
            calculated
        }
    }

    protected inline fun <K, V : Any?, T> MutableMap<K, V>.lookupCacheOrCalculateWithPostCompute(
        key: K, crossinline l: (K) -> Pair<V, T>, postCompute: (V, T) -> Unit
    ): V? {
        return if (containsKey(key)) {
            this[key]
        } else {
            val calculated = l(key)
            this[key] = calculated.first
            postCompute(calculated.first, calculated.second)
            calculated.first
        }
    }

    fun <D> transformTopLevelClasses(transformer: FirTransformer<D>, data: D) {
        val symbols = classCache.values.filterNotNullTo(mutableListOf())
        // TODO: do something with new symbols which can be found during transformation of another symbols
        for (symbol in symbols) {
            if (symbol !is FirClassSymbol) continue
            if (symbol.classId.relativeClassName.parent().isRoot) {
                // Launch for top-level classes only
                symbol.fir.transform<FirElement, D>(transformer, data)
            }
        }
    }
}
