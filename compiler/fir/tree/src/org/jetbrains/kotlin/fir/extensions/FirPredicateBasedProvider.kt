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
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

abstract class FirPredicateBasedProvider : FirSessionComponent {
    abstract fun getSymbolsByPredicate(predicate: DeclarationPredicate): List<FirBasedSymbol<*>>
    abstract fun getOwnersOfDeclaration(declaration: FirDeclaration): List<FirBasedSymbol<*>>?

    /**
     * @return `true` iff file has a top-level annotation from the [FirRegisteredPluginAnnotations.annotations] list.
     * @see FirRegisteredPluginAnnotations.annotations
     */
    abstract fun fileHasPluginAnnotations(file: FirFile): Boolean
    abstract fun matches(predicate: DeclarationPredicate, declaration: FirDeclaration): Boolean

    fun matches(predicate: DeclarationPredicate, declaration: FirBasedSymbol<*>): Boolean {
        return matches(predicate, declaration.fir)
    }

    fun matches(predicates: List<DeclarationPredicate>, declaration: FirDeclaration): Boolean {
        return predicates.any { matches(it, declaration) }
    }

    fun matches(predicates: List<DeclarationPredicate>, declaration: FirBasedSymbol<*>): Boolean {
        return matches(predicates, declaration.fir)
    }

    open fun registerAnnotatedDeclaration(declaration: FirDeclaration, owners: PersistentList<FirDeclaration>) {}
}

@NoMutableState
class FirEmptyPredicateBasedProvider(): FirPredicateBasedProvider() {
    override fun getSymbolsByPredicate(predicate: DeclarationPredicate): List<FirBasedSymbol<*>> = emptyList()

    override fun getOwnersOfDeclaration(declaration: FirDeclaration): List<FirBasedSymbol<*>>? = null

    override fun fileHasPluginAnnotations(file: FirFile): Boolean = false

    override fun matches(predicate: DeclarationPredicate, declaration: FirDeclaration): Boolean = false
}

val FirSession.predicateBasedProvider: FirPredicateBasedProvider by FirSession.sessionComponentAccessor()
