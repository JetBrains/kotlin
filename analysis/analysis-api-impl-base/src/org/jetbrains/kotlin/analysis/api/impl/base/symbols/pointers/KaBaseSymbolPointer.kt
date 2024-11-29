/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import java.lang.ref.WeakReference

abstract class KaBaseSymbolPointer<out S : KaSymbol>(originalSymbol: S?) : KaSymbolPointer<S>() {
    /**
     * Most of the pointers are derived from [KaBaseSymbolPointer] and support weak symbol cache.
     * This means that if the original symbol the pointer is created from is not garbage-collected and still valid on restore,
     * then the [cachedSymbol] will be immediately returned.
     * Otherwise, the new symbol is built from data stored in the pointer, which takes a bit more resources.
     * After each successful build-from-scratch operation, the cached reference is updated and points to the newly constructed symbol.
     */
    final override fun restoreSymbol(analysisSession: KaSession): S? {
        val cached = cachedSymbol?.get()
        val lifetimeToken = cached?.token
        if (lifetimeToken == analysisSession.token) {
            return cached
        } else {
            cachedSymbol = null
        }

        return restoreIfNotCached(analysisSession)?.also {
            if (!it.isLocal()) cachedSymbol = WeakReference(it)
        }
    }

    private var cachedSymbol: WeakReference<@UnsafeVariance S>? = originalSymbol?.let {
        if (!it.isLocal()) WeakReference(it) else null
    }

    private fun KaSymbol.isLocal() =
        ((this as? KaClassLikeSymbol)?.classId?.isLocal != false && (this as? KaCallableSymbol)?.callableId?.isLocal != false)

    protected abstract fun restoreIfNotCached(analysisSession: KaSession): S?
}