/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class AbstractFirSymbolProvider : FirSymbolProvider {
    protected val classCache = mutableMapOf<ClassId, ConeClassLikeSymbol?>()
    protected val callableCache = mutableMapOf<CallableId, List<ConeCallableSymbol>>()
    protected val packageCache = mutableMapOf<FqName, FqName?>()

    protected inline fun <K, V : Any?> MutableMap<K, V>.lookupCacheOrCalculate(key: K, crossinline l: (K) -> V): V? {
        return if (key in this.keys) {
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
        return if (key in this.keys) {
            this[key]
        } else {
            val calculated = l(key)
            this[key] = calculated.first
            postCompute(calculated.first, calculated.second)
            calculated.first
        }
    }
}