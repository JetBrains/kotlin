/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.id

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol

/**
 * Creates the [symbol IDs][FirSymbolId] for the symbols of a session.
 *
 * The factory decides which kind of symbol ID a symbol gets. The compiler registers [FirUniqueSymbolIdFactory], which only creates unique
 * symbol IDs. The Analysis API creates source-based symbol IDs where supported. Code which creates symbols thus doesn't need to distinguish
 * between the compiler and the Analysis API: it requests a [sourceBased] symbol ID if the declaration needs to be discardable, and a
 * [unique] symbol ID otherwise (see [FirSymbolId] for guidance).
 */
abstract class FirSymbolIdFactory : FirSessionComponent {
    /**
     * Creates a [unique symbol ID][FirUniqueSymbolId], both in the compiler and in the Analysis API.
     *
     * This should be used for symbols whose declarations don't need to be discardable (see [FirSymbolId]). It is equivalent to the default
     * constructors of symbols (see [FirBasedSymbol]), but makes the choice explicit where symbol IDs are otherwise created through the
     * factory.
     */
    abstract fun <E : FirDeclaration, S : FirBasedSymbol<E>> unique(): FirSymbolId<S>

    /**
     * Creates a symbol ID based on [sourceElement] where supported, and a unique symbol ID otherwise. In particular, the Analysis API
     * creates source-based symbol IDs, while the compiler creates unique symbol IDs to preserve the identity semantics of symbols.
     *
     * [sourceElement] must identify the declaration exactly: no other declaration of the session may have an equal source element.
     * Otherwise, the symbol IDs of different declarations would be equal (see the constraints in [FirSymbolId]).
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
