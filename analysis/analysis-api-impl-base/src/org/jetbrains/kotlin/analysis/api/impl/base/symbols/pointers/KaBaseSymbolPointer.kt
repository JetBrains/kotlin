/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.symbols.pointers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KaSymbolPointer
import org.jetbrains.kotlin.psi.KtClassInitializer
import java.lang.ref.WeakReference

abstract class KaBaseSymbolPointer<out S : KaSymbol>(originalSymbol: S?) : KaSymbolPointer<S>() {
    /**
     * Most of the pointers are derived from [KaBaseSymbolPointer] and support weak symbol cache.
     * This means that if the original symbol the pointer is created from is not garbage-collected and still valid on restore,
     * then the [cachedSymbol] will be immediately returned.
     * Otherwise, the new symbol is built from data stored in the pointer, which takes a bit more resources.
     * After each successful build-from-scratch operation, the cached reference is updated and points to the newly constructed symbol.
     *
     * Currently, symbol caching is only supported for non-local declarations.
     * The invalidation process for local declarations is bulky, as the lifetime token stays the same on in-block modifications.
     */
    final override fun restoreSymbol(analysisSession: KaSession): S? {
        @Suppress("UNCHECKED_CAST")
        val cached = (cachedSymbol as? WeakReference<*>)?.get() as? S
        val lifetimeToken = cached?.token
        if (lifetimeToken == analysisSession.token) {
            return cached
        }

        return restoreIfNotCached(analysisSession)?.also {
            runCaching(it)
        }
    }

    private var cachedSymbol: Any? = originalSymbol?.let {
        runCaching(it)
    }

    private fun runCaching(symbol: S) {
        if (cachedSymbol === IS_LOCAL) return
        if (!symbol.isLocal) cachedSymbol = WeakReference(symbol) else IS_LOCAL
    }

    private companion object {
        const val IS_LOCAL = "IS_LOCAL"
    }

    private val KaSymbol.isLocal: Boolean
        get() = when(this) {
            is KaCallableSymbol -> this.callableId?.isLocal == true
            is KaClassLikeSymbol -> this.classId?.isLocal == true
            is KaClassInitializerSymbol -> (this.psi as? KtClassInitializer)?.containingDeclaration?.getClassId()?.isLocal == true
            is KaScriptSymbol, is KaFileSymbol, is KaPackageSymbol -> false
            else -> true
        }

    protected abstract fun restoreIfNotCached(analysisSession: KaSession): S?
}