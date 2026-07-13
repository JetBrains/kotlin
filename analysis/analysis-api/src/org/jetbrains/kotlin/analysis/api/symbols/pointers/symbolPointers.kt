/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

/**
 * Returns the restored [KaSymbol] (possibly a new symbol instance) if the pointer is still valid, or `null` otherwise.
 */
context(session: KaSession)
public fun <S : KaSymbol> KaSymbolPointer<S>.restoreSymbol(): S? = session.withValidityAssertion {
    @OptIn(KaImplementationDetail::class)
    restoreSymbol(session)
}
