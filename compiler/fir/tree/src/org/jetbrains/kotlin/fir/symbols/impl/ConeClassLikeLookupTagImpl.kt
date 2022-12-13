/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.util.WeakPair
import org.jetbrains.kotlin.name.ClassId
import java.text.DecimalFormat

@RequiresOptIn
annotation class LookupTagInternals

object LookupTagBoundSymbolTracker : CacheTracker("ConeClassLikeLookupTagImpl")

abstract class CacheTracker(val name: String) {

    private var hits: Long = 0
    private var misses: Long = 0
    fun hit() {
        hits++
    }

    fun miss() {
        misses++
    }

    fun print() {
        val numberFormat = DecimalFormat()
        numberFormat.maximumFractionDigits = 3
        println("$name: hits = ${hits}, misses = ${misses}, ratio = ${numberFormat.format(misses.toDouble() / (misses + hits))}")
    }

    init {
        allTrackers.add(this)
    }

    companion object {
        val allTrackers = mutableListOf<CacheTracker>()
        fun print() {
            allTrackers.forEach { it.print() }
        }
    }
}

class ConeClassLikeLookupTagImpl(override val classId: ClassId) : ConeClassLikeLookupTag() {

    init {
        assert(!classId.isLocal) { "You should use ConeClassLookupTagWithFixedSymbol for local $classId!" }
    }

    private var boundSymbol: WeakPair<FirSession, FirClassLikeSymbol<*>?>? = null

    fun bindTo(useSiteSession: FirSession, value: FirClassLikeSymbol<*>?) {
        boundSymbol = WeakPair(useSiteSession, value)
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

    override fun toSymbol(useSiteSession: FirSession): FirClassLikeSymbol<*>? {
        val bound = boundSymbol
        if (bound == null || bound.first !== useSiteSession) {
            return findTargetSymbol(useSiteSession)
        }
        return bound.second
    }

    private fun findTargetSymbol(useSiteSession: FirSession): FirClassLikeSymbol<*>? {
        val symbol = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        boundSymbol = WeakPair(useSiteSession, symbol)
        return symbol
    }
}
