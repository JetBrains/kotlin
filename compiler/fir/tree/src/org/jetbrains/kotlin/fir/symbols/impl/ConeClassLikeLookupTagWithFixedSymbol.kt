/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.name.ClassId

/**
 * TODO (marco): Document: The lookup tag saves a symbol ID internally to avoid strong references to symbols where it is not necessary.
 *  While lookup tags with fixed symbols "should" only be used by other local declarations, in reality, they can for example be used as
 *  cache keys in session components like `FirIntersectionOverrideStorage`. As long as the overall local context is still alive, the local
 *  declaration's FIR file will remain referenced, so the symbol ID should not need to restore its symbol during this period.
 *  Also note: The symbol reference in the lookup tag can be classified as a cross reference, which means it should be replaced with a
 *  symbol ID.
 */
class ConeClassLikeLookupTagWithFixedSymbol(
    override val classId: ClassId,
    val symbolId: FirSymbolId<FirClassLikeSymbol<*>>,
) : ConeClassLikeLookupTag() {
    /**
     * Constructs a [ConeClassLikeLookupTagWithFixedSymbol] from a [FirClassLikeSymbol] instead of the [FirSymbolId] stored by the lookup
     * tag.
     *
     * Note: This constructor should not be used for a symbol which is currently initializing, as its symbol ID might be `null`.
     */
    constructor(classId: ClassId, symbol: FirClassLikeSymbol<*>) : this(classId, symbol.symbolId)

    val symbol: FirClassLikeSymbol<*>
        get() = symbolId.symbol

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeClassLikeLookupTagWithFixedSymbol

        if (symbolId != other.symbolId) return false

        return true
    }

    override fun hashCode(): Int {
        return symbolId.hashCode()
    }
}
