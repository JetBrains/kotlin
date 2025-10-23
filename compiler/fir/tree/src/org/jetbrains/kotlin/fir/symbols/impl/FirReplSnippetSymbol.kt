/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.symbols.impl

import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.id.FirSymbolId

class FirReplSnippetSymbol(
    override val symbolId: FirSymbolId<FirReplSnippetSymbol>,
    val snippetClassSymbol: FirRegularClassSymbol,
) : FirBasedSymbol<FirReplSnippet>(symbolId) {
    override fun toString(): String = "${this::class.simpleName} ${snippetClassSymbol.name.asString()}"
}
