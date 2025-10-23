/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.id

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * TODO (marco): Document.
 *
 * TODO (marco): We can add a third category: Symbol IDs which are source-based but never lose their referent because it's impossible to
 *  restore them. This would still grant source-based equality but without issues around restoration. Example: `FirCodeFragmentSymbol`.
 */
abstract class FirSymbolIdFactory : FirSessionComponent {
    /**
     * Idea: Endpoint for creating a symbol ID which is ALWAYS unique, both in the compiler and the Analysis API. This should be used for
     * symbols where we legitimately don't have an anchor. The light tree builder can also always use this endpoint, since it's only used by
     * the compiler.
     *
     * TODO (marco): Document.
     */
    abstract fun <E : FirDeclaration, S : FirBasedSymbol<E>> unique(): FirSymbolId<S>

    /**
     * Idea: A symbol ID which is source-based in the Analysis API, but still unique in the compiler to preserve identity semantics.
     *
     * TODO (marco): Document.
     */
    abstract fun <E : FirDeclaration, S : FirBasedSymbol<E>> sourceBased(sourceElement: KtSourceElement): FirSymbolId<S>

    /**
     * Returns a [sourceBased] symbol ID when [sourceElement] is non-null, and a [unique] symbol ID otherwise.
     *
     * The function should be used in generic cases where we don't know whether a source element exists. For example, [sourceBasedOrUnique]
     * might be used in a code generation endpoint which is called not only from PSI/light tree to FIR, but also from library metadata
     * deserialization. It might also be called from transformers.
     *
     * If misused, this function can lead to a situation where we're expecting a source-based symbol ID, but a unique symbol ID is produced.
     * However, if the unique symbol ID is not consistent with our caches, the mistake will likely be discovered by symbol ID constraint
     * checks in Analysis API tests.
     */
    fun <E : FirDeclaration, S : FirBasedSymbol<E>> sourceBasedOrUnique(sourceElement: KtSourceElement?): FirSymbolId<S> =
        if (sourceElement != null) sourceBased(sourceElement) else unique()
}

val FirSession.symbolIdFactory: FirSymbolIdFactory by FirSession.sessionComponentAccessor()
