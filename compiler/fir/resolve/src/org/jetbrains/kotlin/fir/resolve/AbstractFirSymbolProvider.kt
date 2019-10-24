/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class AbstractFirSymbolProvider<C : FirClassLikeSymbol<*>> : FirSymbolProvider() {
    protected val classCache = HashMap<ClassId, C?>()
    protected val topLevelCallableCache = HashMap<CallableId, List<FirCallableSymbol<*>>>()
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

    protected inline fun <K, V : Any, T> MutableMap<K, V?>.lookupCacheOrCalculateWithPostCompute(
        key: K, crossinline l: (K) -> Pair<V?, T>, postCompute: (V, T) -> Unit
    ): V? {
        return if (containsKey(key)) {
            this[key]
        } else {
            val calculated = l(key)
            this[key] = calculated.first
            calculated.first?.let { first -> postCompute(first, calculated.second) }
            calculated.first
        }
    }
}
