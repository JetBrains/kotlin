/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol

interface BirClassifierEqualityChecker {
    fun areEqual(left: BirClassifierSymbol, right: BirClassifierSymbol): Boolean

    fun getHashCode(symbol: BirClassifierSymbol): Int
}

object FqNameEqualityChecker : BirClassifierEqualityChecker {
    override fun areEqual(left: BirClassifierSymbol, right: BirClassifierSymbol): Boolean =
        left === right ||
                left.signature != null && left.signature == right.signature

    override fun getHashCode(symbol: BirClassifierSymbol): Int =
        symbol.signature?.hashCode() ?: symbol.hashCode()
}
