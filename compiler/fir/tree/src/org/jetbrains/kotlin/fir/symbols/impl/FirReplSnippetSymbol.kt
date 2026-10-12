/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId
import org.jetbrains.kotlin.fir.symbols.id.FirUniqueSymbolId

class FirReplSnippetSymbol(
    symbolId: FirSymbolId<FirReplSnippetSymbol>,
    val snippetClassSymbol: FirRegularClassSymbol,
) : FirBasedSymbol<FirReplSnippet>(symbolId) {
    /**
     * Creates a [FirReplSnippetSymbol] with a [unique symbol ID][FirUniqueSymbolId].
     * See [FirBasedSymbol] for when to use this default constructor.
     */
    constructor(snippetClassSymbol: FirRegularClassSymbol) : this(FirUniqueSymbolId(), snippetClassSymbol)

    override fun toString(): String = "${this::class.simpleName} ${snippetClassSymbol.name.asString()}"
}
