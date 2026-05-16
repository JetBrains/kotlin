/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.extensions

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import kotlin.reflect.KClass

/**
 * Marker returned by [FirReplHistoryProvider.getSnippetImports] when the provider has no opinion
 * on a snippet's imports (e.g. the default in-memory provider, which expects the FIR-provider's
 * `getFirReplSnippetContainerFile(snippet)` lookup to supply them). Distinct from an empty list,
 * which **does** assert "this snippet has no imports".
 */

abstract class FirReplSnippetResolveExtension(
    session: FirSession,
) : FirExtensionSessionComponent(session) {
    override val componentClass: KClass<out FirExtensionSessionComponent>
        get() = FirReplSnippetResolveExtension::class

    abstract fun getSnippetDefaultImports(sourceFile: KtSourceFile, snippet: FirReplSnippet): List<FirImport>?

    abstract fun getSnippetScope(currentSnippet: FirReplSnippet, useSiteSession: FirSession): FirScope?

    abstract fun updateResolved(snippet: FirReplSnippet)
}

val FirSession.replSnippetResolveExtension: FirReplSnippetResolveExtension? by FirSession.nullableSessionComponentAccessor()

abstract class FirReplHistoryProvider : FirSessionComponent {
    abstract fun getSnippets(): Iterable<FirReplSnippetSymbol>
    abstract fun putSnippet(symbol: FirReplSnippetSymbol)
    abstract fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean
    abstract fun getSnippetCount(): Int

    /**
     * Optional hook: return the list of [FirImport]s authored in [symbol]'s containing file.
     *
     * Returning `null` (the default) means "I don't know — fall back to the
     * `FirProvider.getFirReplSnippetContainerFile(symbol).imports` path that the in-memory REPL
     * builds during recordFile". Returning an empty list **asserts** "this prior snippet had no
     * imports".
     *
     * Existence rationale: the stateless / artifact-backed provider reconstructs a
     * [FirReplSnippetSymbol] from a deserialised wrapper class — there is no recorded
     * `FirFile`, so the firProvider lookup yields `null` and prior snippets' imports are dropped
     * cross-snippet. This hook lets the artifact-backed provider materialise the imports directly
     * from the sidecar's `ImportEntry` list without having to also synthesise + register a
     * `FirFile` (which would have to satisfy a much larger surface — `packageDirective`,
     * `declarations`, file-level annotations, source-file binding) just to carry imports.
     */
    open fun getSnippetImports(symbol: FirReplSnippetSymbol): List<FirImport>? = null
}

