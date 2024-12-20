/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers.KaBaseCachedSymbolPointer.Companion.NOT_CACHED
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import java.lang.ref.WeakReference

abstract class KaBaseCachedSymbolPointer<out S : KaSymbol>(originalSymbol: S?) : KaSymbolPointer<S>() {
    /**
     * This property can have three values:
     *
     * 1. **null** – cache is not initialized, so it can be initialized with the next [restoreSymbol]
     * 2. [NOT_CACHED] – the symbol cannot be cached, so [restoreSymbol] should delegate directly to [restoreIfNotCached]
     * 3. [WeakReference] – the symbol is cacheable, [cachedSymbol] stores the result of the latest [restoreIfNotCached] call
     */
    private var cachedSymbol: Any? = null

    init {
        originalSymbol?.let(::cacheWithIsCacheableCheck)
    }

    /**
     * Most of the pointers are derived from [KaBaseCachedSymbolPointer] and support weak symbol cache.
     * This means that if the original symbol the pointer is created from is not garbage-collected and still valid on restore,
     * then the [cachedSymbol] will be immediately returned.
     * Otherwise, the new symbol is built from data stored in the pointer, which takes a bit more resources.
     * After each successful build-from-scratch operation, the cached reference is updated and points to the newly constructed symbol.
     *
     * Currently, symbol caching is only supported for non-local declarations.
     * The invalidation process for local declarations is bulky, as the lifetime token stays the same on in-block modifications.
     */
    final override fun restoreSymbol(analysisSession: KaSession): S? = when (val cachedSymbol = cachedSymbol) {
        is WeakReference<*> -> {
            val value = cachedSymbol.get()
            @Suppress("UNCHECKED_CAST")
            if (value != null && (value as S).token == analysisSession.token) {
                value
            } else {
                restoreIfNotCached(analysisSession).also {
                    this.cachedSymbol = WeakReference(it)
                }
            }
        }
        NOT_CACHED -> restoreIfNotCached(analysisSession)
        null -> restoreIfNotCached(analysisSession)?.also(::cacheWithIsCacheableCheck)
        else -> errorWithAttachment("Unexpected value: ${cachedSymbol::class.simpleName}") {
            withEntry("cachedSymbol", cachedSymbol.toString())
        }
    }

    private fun cacheWithIsCacheableCheck(symbol: S) {
        cachedSymbol = if (symbol.isCacheable) WeakReference(symbol) else NOT_CACHED
    }

    companion object {
        private val NOT_CACHED = Any()

        internal val KaSymbol.isCacheable: Boolean
            get() = when (this) {
                is KaConstructorSymbol -> this.containingClassId?.isLocal == false
                is KaCallableSymbol -> this.callableId?.isLocal == false
                is KaClassLikeSymbol -> this.classId?.isLocal == false
                is KaClassInitializerSymbol -> (this.psi as? KtClassInitializer)?.containingDeclaration?.getClassId()?.isLocal == false
                is KaScriptSymbol, is KaFileSymbol, is KaPackageSymbol -> true
                else -> false
            }
    }

    protected abstract fun restoreIfNotCached(analysisSession: KaSession): S?
}