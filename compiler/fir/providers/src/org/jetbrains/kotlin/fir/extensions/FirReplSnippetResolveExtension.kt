/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.name.ClassId
import kotlin.reflect.KClass

abstract class FirReplSnippetResolveExtension(
    session: FirSession,
) : FirExtensionSessionComponent(session) {
    override val componentClass: KClass<out FirExtensionSessionComponent>
        get() = FirReplSnippetResolveExtension::class

    abstract fun getSnippetHistoryImports(snippet: FirReplSnippet): List<FirImport>

    abstract fun getSnippetScope(currentSnippet: FirReplSnippet, useSiteSession: FirSession): FirScope?

    abstract fun updateResolved(snippet: FirReplSnippet)

    abstract val replHistoryProvider: FirReplHistoryProvider
}

val FirSession.replSnippetResolveExtension: FirReplSnippetResolveExtension? by FirSession.nullableSessionComponentAccessor()

val FirSession.replHistoryProvider: FirReplHistoryProvider?
    get() = replSnippetResolveExtension?.replHistoryProvider

fun FirSession.containingReplSnippet(symbol: FirBasedSymbol<*>): FirReplSnippetSymbol? =
    replHistoryProvider?.getContainingSnippet(symbol)

abstract class FirReplHistoryProvider : FirSessionComponent {
    abstract fun getSnippets(): Iterable<FirReplSnippetSymbol>
    abstract fun putSnippet(symbol: FirReplSnippetSymbol)

    /**
     * Imported snippets are visible to the snippets that follow them, but are not counted by [getSnippetCount].
     */
    open fun putImportedSnippet(symbol: FirReplSnippetSymbol): Unit = putSnippet(symbol)

    abstract fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean
    abstract fun getSnippetCount(): Int
    open fun getSnippetImports(symbol: FirReplSnippetSymbol): List<FirImport>? = null

    /**
     * The snippet whose class declares [declaration] directly. A class declared inside a snippet-level class, and the members
     * of both, have no containing snippet: they are accessed through an instance of their own class, and reporting a snippet
     * for them makes Fir2Ir replace that instance with an error expression.
     */
    fun getContainingSnippet(declaration: FirDeclaration): FirReplSnippetSymbol? {
        if (declaration.isReplSnippetDeclaration != true) return null
        val containerClassId = declaration.snippetContainerClassId() ?: return null
        return getSnippets().firstOrNull { it.snippetClassSymbol.classId == containerClassId }
    }

    @OptIn(SymbolInternals::class)
    fun getContainingSnippet(symbol: FirBasedSymbol<*>): FirReplSnippetSymbol? = getContainingSnippet(symbol.fir)

    private fun FirDeclaration.snippetContainerClassId(): ClassId? = when (this) {
        is FirCallableDeclaration -> containingClassLookupTag()?.classId
        is FirClassLikeDeclaration -> symbol.classId.outerClassId
        else -> null
    }
}

