/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.scopes

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.signatures.KtCallableSignature


public interface KtTypeScope : KtScopeLike {

    /**
     * Return a sequence of [KtCallableSymbol] which current scope contain if declaration name matches [nameFilter]
     */
    public fun getCallableSignatures(nameFilter: KtScopeNameFilter = { true }): Sequence<KtCallableSignature<*>>

    /**
     * Return a sequence of [KtClassifierSymbol] which current scope contain if declaration name matches [nameFilter]
     */
    public fun getClassifierSymbols(nameFilter: KtScopeNameFilter = { true }): Sequence<KtClassifierSymbol>

    /**
     * Return a sequence of [KtConstructorSymbol] which current scope contain
     */
    public fun getConstructors(): Sequence<KtConstructorSymbol>
}

