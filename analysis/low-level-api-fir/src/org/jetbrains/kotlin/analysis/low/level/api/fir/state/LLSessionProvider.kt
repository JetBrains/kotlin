/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.project.structure.KtModule

class LLSessionProvider(
    val useSiteModule: KtModule,
    private val useSiteSessionFactory: (KtModule) -> LLFirSession
) {
    /**
     * The [LLFirSession] must be strongly reachable from the resolvable session and ultimately the `KtFirAnalysisSession` so that soft
     * reference garbage collection doesn't collect the [LLFirSession] without collecting its dependent `KtFirAnalysisSession`. See
     * [LLFirSession] for more details.
     */
    val useSiteSession: LLFirSession by lazy(LazyThreadSafetyMode.PUBLICATION) { useSiteSessionFactory(useSiteModule) }

    /**
     * Returns an [LLFirSession] for the [module].
     * For a binary module, the resulting session will be a binary (non-resolvable) one.
     */
    fun getSession(module: KtModule): LLFirSession {
        return getSession(module, preferBinary = true)
    }

    /**
     * Returns an analyzable [LLFirSession] for the module.
     * For a binary module, the resulting session will still be a resolvable one.
     *
     * Note: prefer using [getSession] unless you need to perform resolution actively.
     * Resolvable sessions for libraries are much less performant.
     */
    fun getResolvableSession(module: KtModule): LLFirResolvableModuleSession {
        return getSession(module, preferBinary = false) as LLFirResolvableModuleSession
    }

    private fun getSession(module: KtModule, preferBinary: Boolean): LLFirSession {
        if (module == useSiteModule) {
            return useSiteSession
        }

        val cache = LLFirSessionCache.getInstance(module.project)
        return cache.getSession(module, preferBinary)
    }
}