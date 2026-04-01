/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.psi.util.CachedValue
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.references.KaFirReference
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.caches.NullableCaffeineCache
import org.jetbrains.kotlin.analysis.api.platform.caches.withStatsCounter
import org.jetbrains.kotlin.analysis.api.resolution.KaCallResolutionAttempt
import org.jetbrains.kotlin.analysis.api.resolution.KaSymbolResolutionAttempt
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.LLFirInBlockModificationTracker
import org.jetbrains.kotlin.analysis.low.level.api.fir.statistics.LLStatisticsService
import org.jetbrains.kotlin.analysis.utils.caches.softCachedValue
import org.jetbrains.kotlin.psi.KtElement

/**
 * This is a dedicated place for caches stored directly inside [KaSession].
 *
 * For [KaSessionComponent] it is possible to have such a cache directly near the use site,
 * but for non-components it is impossible if they are not a part of the session.
 * For instance, [KaFirReference] is not a component, so this storage can be used to have all
 * [KaSession] benefits such as active invalidation inside
 * [KaFirReference.resolveToSymbols] implementation.
 *
 * In addition, this storage provides entry points like [softCachedValueWithPsiKey] to unify the UX.
 */
internal class KaFirInternalCacheStorage(private val analysisSession: KaFirSession) {
    @KaCachedService
    private val statisticsService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLStatisticsService.getInstance(project)
    }

    private val project get() = analysisSession.project

    val resolveCallCache: CachedValue<NullableCaffeineCache<KtElement, KaCallResolutionAttempt>> by lazy {
        softCachedValueWithPsiKey {
            NullableCaffeineCache {
                it.withStatsCounter(statisticsService?.analysisSessions?.resolveCallCacheStatsCounter)
            }
        }
    }

    val resolveSymbolCache: CachedValue<NullableCaffeineCache<KtElement, KaSymbolResolutionAttempt>> by lazy {
        softCachedValueWithPsiKey {
            NullableCaffeineCache {
                it.withStatsCounter(statisticsService?.analysisSessions?.resolveSymbolCacheStatsCounter)
            }
        }
    }

    val resolveToSymbolsCache: CachedValue<Cache<KaFirReference, Collection<KaSymbol>>> by lazy {
        softCachedValueWithPsiKey {
            Caffeine.newBuilder()
                .withStatsCounter(statisticsService?.analysisSessions?.resolveToSymbolsCacheStatsCounter)
                .build()
        }
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
