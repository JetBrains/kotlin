/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(SymbolInternals::class)

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.getOwnerLookupTag
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals

@OptIn(SymbolInternals::class)
open class FirDeclarationWithContext<out T : FirDeclaration>(
    val declaration: T,
    val context: CheckerContext,
) {
    val session get() = context.session

    // For companions getContainingClassSymbol() returns null
    private val containingClass
        get(): FirClassLikeDeclaration? {
            declaration.getContainingClassSymbol(session)?.fir?.let {
                return it
            }

            return if (declaration is FirClass) {
                declaration.symbol.getOwnerLookupTag()?.toSymbol(session)?.fir
            } else {
                null
            }
        }

    open val parentClass get() = containingClass?.let { FirDeclarationWithContext(it, context) }

    open val container: FirDeclaration? get() = containingClass

    open val parent: FirDeclarationWithContext<FirDeclaration>? get() = parentClass
}

class FirDeclarationWithParents<out T : FirDeclaration>(
    declaration: T,
    val parents: List<FirDeclaration>,
    context: CheckerContext,
) : FirDeclarationWithContext<T>(declaration, context) {
    override val container get() = parents.lastOrNull()

    override val parent get() = container?.let { FirDeclarationWithParents(it, parents.withoutLast(), context) }

    @Suppress("UNCHECKED_CAST")
    private val containingClass get() = parents.withIndex().findLast { (_, it) -> it is FirClass } as? IndexedValue<FirClass>

    override val parentClass
        get() = containingClass?.let { (index, it) ->
            FirDeclarationWithParents(it, parents.subList(0, index), context)
        }
}

fun <T : FirDeclaration> FirDeclarationWithContext<*>.withSameParents(another: T) = when (this) {
    is FirDeclarationWithParents<*> -> FirDeclarationWithParents(another, parents.withoutLast(), context)
    else -> FirDeclarationWithContext(another, context)
}
