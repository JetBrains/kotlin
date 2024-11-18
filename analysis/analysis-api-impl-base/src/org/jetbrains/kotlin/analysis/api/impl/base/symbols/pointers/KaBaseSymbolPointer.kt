/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import java.lang.ref.WeakReference

abstract class KaBaseSymbolPointer<out S : KaSymbol> : KaSymbolPointer<S>() {
    override fun restoreSymbol(analysisSession: KaSession): S? {
        val cached = cachedSymbol?.get()
        val lifetimeToken = cached?.token
        if (lifetimeToken?.isValid() == true && lifetimeToken.isAccessible()) {
            return cached
        } else {
            cachedSymbol = null
        }

        return restoreIfNotCached(analysisSession)?.also {
            cachedSymbol = WeakReference(it)
        }
    }

    protected abstract var cachedSymbol: WeakReference<@UnsafeVariance S>?

    protected abstract fun restoreIfNotCached(analysisSession: KaSession): S?
}