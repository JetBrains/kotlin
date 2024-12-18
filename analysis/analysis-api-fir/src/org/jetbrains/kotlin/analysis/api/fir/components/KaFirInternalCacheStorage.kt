/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.psi.util.CachedValue
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.platform.utils.NullableConcurrentCache
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolBasedReference
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirInBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.softCachedValue
import org.jetbrains.kotlin.psi.KtElement
import java.util.concurrent.ConcurrentHashMap

/**
 * This is a dedicated place for caches stored directly inside [KaSession].
 *
 * For [KaSessionComponent] it is possible to have such a cache directly near the use site,
 * but for non-components it is impossible if they are not a part of the session.
 * For instance, [KaSymbolBasedReference] is not a component, so this storage can be used to have all
 * [KaSession] benefits such as active invalidation inside
 * [KaSymbolBasedReference.resolveToSymbols] implementation.
 *
 * In addition, this storage provides entry points like [softCachedValueWithPsiKey] to unify the UX.
 */
internal class KaFirInternalCacheStorage(private val analysisSession: KaFirSession) {
    private val project get() = analysisSession.project

    val resolveToCallCache: CachedValue<NullableConcurrentCache<KtElement, KaCallInfo?>> by lazy {
        softCachedValueWithPsiKey {
            NullableConcurrentCache<KtElement, KaCallInfo?>()
        }
    }

    val resolveToSymbolsCache: CachedValue<ConcurrentHashMap<KaSymbolBasedReference, Collection<KaSymbol>>> by lazy {
        softCachedValueWithPsiKey { ConcurrentHashMap<KaSymbolBasedReference, Collection<KaSymbol>>() }
    }

    /**
     * The lifetime of this cache is the same as the corresponding [org.jetbrains.kotlin.analysis.api.KaSession],
     * so it doesn't require additional invalidation.
     *
     * The only case where we need to invalidate FIR without the containing session being invalidated is
     * [in-block modification][org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirDeclarationModificationService].
     */
    private inline fun <T> softCachedValueWithPsiKey(crossinline createValue: () -> T): CachedValue<T> {
        return softCachedValue(project, LLFirInBlockModificationTracker.getInstance(project)) {
            createValue()
        }
    }
}
