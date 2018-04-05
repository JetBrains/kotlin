/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

abstract class AbstractFirSymbolProvider : FirSymbolProvider {
    protected val classCache = mutableMapOf<ClassId, ConeSymbol?>()
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
}