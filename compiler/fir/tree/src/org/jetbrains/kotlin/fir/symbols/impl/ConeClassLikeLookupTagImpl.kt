/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeClassLikeLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.util.PrivateForInline
import java.lang.ref.WeakReference

@RequiresOptIn
annotation class LookupTagInternals

class ConeClassLikeLookupTagImpl(override val classId: ClassId) : ConeClassLikeLookupTag() {
    init {
        @OptIn(ClassIdBasedLocality::class)
        assert(!classId.isLocal) { "You should use ${ConeClassLikeLookupTagWithFixedSymbol::class.simpleName} for local $classId!" }
    }

    @PrivateForInline
    var boundSymbol: BoundSymbol? = null

    @LookupTagInternals
    @OptIn(PrivateForInline::class)
    fun bindSymbolToLookupTag(session: FirSession, symbol: FirClassLikeSymbol<*>?) {
        boundSymbol = BoundSymbol(session, symbol)
    }

    @LookupTagInternals
    @OptIn(PrivateForInline::class)
    inline fun withBoundSymbol(session: FirSession, f: (FirClassLikeSymbol<*>?) -> Unit) {
        val boundSymbol = boundSymbol?.takeIf { it.session === session } ?: return
        if (boundSymbol.isNullSymbol) {
            f(null)
        } else {
            // The bound symbol might have been collected, so it could be `null` now. In that case, we should NOT assume we have a `null`
            // symbol, since initially the symbol was non-null. So we must not call `f(null)` in this branch.
            val symbol = boundSymbol.symbol
            if (symbol != null) {
                f(symbol)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeClassLikeLookupTagImpl

        if (classId != other.classId) return false

        return true
    }

    override fun hashCode(): Int {
        return classId.hashCode()
    }

    // TODO (marco): Document: Now that symbols can be weakly referenced, there is a difference between "we found a null symbol" and "the
    //  previous weakly referenced symbol has been garbage-collected." This introduces an ambiguity on `null`, since previously we could
    //  assume that "symbol == null --> no symbol was found."
    // TODO (marco): Consider splitting this class into two variants, one for the `null` symbol and the other for the non-null symbol?
    @PrivateForInline
    class BoundSymbol(session: FirSession, symbol: FirClassLikeSymbol<*>?) {
        private val sessionReference = WeakReference(session)
        private val symbolReference = if (symbol != null) WeakReference(symbol) else null

        val session: FirSession?
            get() = sessionReference.get()

        val symbol: FirClassLikeSymbol<*>?
            get() = symbolReference?.get()

        val isNullSymbol: Boolean = symbolReference == null
    }
}
