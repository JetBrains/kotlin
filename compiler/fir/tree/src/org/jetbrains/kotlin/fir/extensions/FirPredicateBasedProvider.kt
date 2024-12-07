/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import kotlinx.collections.immutable.PersistentList
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.predicate.AbstractPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

abstract class FirPredicateBasedProvider : FirSessionComponent {
    /**
     * @return list of all declarations from compiled source module which are matched to [predicate]
     */
    abstract fun getSymbolsByPredicate(predicate: LookupPredicate): List<FirBasedSymbol<*>>

    /**
     * @return list of all parents of [declaration]
     */
    abstract fun getOwnersOfDeclaration(declaration: FirDeclaration): List<FirBasedSymbol<*>>?

    /**
     * @return `true` if file has a top-level annotation from the [FirRegisteredPluginAnnotations.annotations] list.
     * @see FirRegisteredPluginAnnotations.annotations
     */
    abstract fun fileHasPluginAnnotations(file: FirFile): Boolean

    /**
     * @return if [declaration] matches [predicate] or not
     */
    abstract fun matches(predicate: AbstractPredicate<*>, declaration: FirDeclaration): Boolean

    /**
     * @return if [declaration] matches [predicate] or not
     */
    fun matches(predicate: AbstractPredicate<*>, declaration: FirBasedSymbol<*>): Boolean {
        return matches(predicate, declaration.fir)
    }

    /**
     * @return if [declaration] matches any predicate from [predicates] or not
     */
    fun matches(predicates: List<AbstractPredicate<*>>, declaration: FirDeclaration): Boolean {
        return predicates.any { matches(it, declaration) }
    }

    /**
     * @return if [declaration] matches any predicate from [predicates] or not
     */
    fun matches(predicates: List<AbstractPredicate<*>>, declaration: FirBasedSymbol<*>): Boolean {
        return matches(predicates, declaration.fir)
    }

    /**
     * Utility method which should not be used from plugins
     */
    @FirExtensionApiInternals
    open fun registerAnnotatedDeclaration(declaration: FirDeclaration, owners: PersistentList<FirDeclaration>) {}
}

@NoMutableState
object FirEmptyPredicateBasedProvider : FirPredicateBasedProvider() {
    override fun getSymbolsByPredicate(predicate: LookupPredicate): List<FirBasedSymbol<*>> = emptyList()

    override fun getOwnersOfDeclaration(declaration: FirDeclaration): List<FirBasedSymbol<*>>? = null

    override fun fileHasPluginAnnotations(file: FirFile): Boolean = false

    override fun matches(predicate: AbstractPredicate<*>, declaration: FirDeclaration): Boolean = false
}

val FirSession.predicateBasedProvider: FirPredicateBasedProvider by FirSession.sessionComponentAccessor()
