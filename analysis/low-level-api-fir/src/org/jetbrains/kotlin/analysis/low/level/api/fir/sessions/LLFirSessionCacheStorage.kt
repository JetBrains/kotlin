/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable.CleanableSoftValueReferenceCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable.CleanableValueReferenceCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable.CleanableWeakValueReferenceCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.cleanable.ValueReferenceCleaner

/**
 * A type of cache which is used by [LLFirSessionCache] to store [LLFirSession]s.
 *
 * Removal from the session storage invokes the [LLFirSession]'s cleaner, which marks the session as invalid and disposes any disposables
 * registered with the session's disposable.
 */
internal typealias SessionStorage = CleanableValueReferenceCache<KaModule, LLFirSession>

/**
 * Holds all the caches which are operated by [LLFirSessionCache].
 */
@LLFirInternals
class LLFirSessionCacheStorage(
    val sourceCache: SessionStorage,
    val binaryCache: SessionStorage,

    /**
     * A cache for the binary sessions of [org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule]s.
     *
     * We keep this cache separate from [binaryCache] for the following reasons:
     *
     * 1. We usually have to invalidate *all* fallback dependencies sessions at once. It's cheaper to clear a whole cache instead of
     *    traversing the binary cache.
     * 2. There is no sense in holding fallback dependencies on soft references, as they exist for a single use-site resolvable library
     *    session. Furthermore, such a session can grow arbitrarily large as it spans (almost) all libraries in the project.
     */
    val libraryFallbackDependenciesCache: SessionStorage,

    val danglingFileSessionCache: SessionStorage,
    val unstableDanglingFileSessionCache: SessionStorage,
    val getCleaner: (LLFirSession) -> ValueReferenceCleaner<LLFirSession>,
) {

    fun createCopy(): LLFirSessionCacheStorage {
        return LLFirSessionCacheStorage(
            sourceCache = sourceCache.createCopy(),
            binaryCache = binaryCache.createCopy(),
            libraryFallbackDependenciesCache = libraryFallbackDependenciesCache.createCopy(),
            danglingFileSessionCache = danglingFileSessionCache.createCopy(),
            unstableDanglingFileSessionCache = unstableDanglingFileSessionCache.createCopy(),
            getCleaner = getCleaner,
        )
    }

    companion object {
        fun createEmpty(
            getCleaner: (LLFirSession) -> ValueReferenceCleaner<LLFirSession>,
        ): LLFirSessionCacheStorage {
            return LLFirSessionCacheStorage(
                sourceCache = CleanableWeakValueReferenceCache(getCleaner = getCleaner),
                binaryCache = CleanableSoftValueReferenceCache(getCleaner = getCleaner),
                libraryFallbackDependenciesCache = CleanableWeakValueReferenceCache(getCleaner = getCleaner),
                danglingFileSessionCache = CleanableWeakValueReferenceCache(getCleaner = getCleaner),
                unstableDanglingFileSessionCache = CleanableWeakValueReferenceCache(getCleaner = getCleaner),
                getCleaner = getCleaner,
            )
        }
    }
}