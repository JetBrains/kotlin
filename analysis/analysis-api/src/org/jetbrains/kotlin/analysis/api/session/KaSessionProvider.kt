/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtElement

/**
 * Provides [KaSession]s by use-site [KtElement]s or [KaModule]s.
 *
 * This provider should not be used directly.
 * Please use [analyze][org.jetbrains.kotlin.analysis.api.analyze] or [analyzeCopy][org.jetbrains.kotlin.analysis.api.analyzeCopy] instead.
 */
@OptIn(KaAnalysisApiInternals::class)
public abstract class KaSessionProvider(public val project: Project) : Disposable {
    public abstract fun getAnalysisSession(useSiteElement: KtElement): KaSession

    public abstract fun getAnalysisSession(useSiteModule: KaModule): KaSession

    // The `analyse` functions affect binary compatibility as they are inlined with every `analyze` call. To avoid breaking binary
    // compatibility, their implementations should not be changed unless absolutely necessary. It should be possible to put most
    // functionality into `beforeEnteringAnalysis` and/or `afterLeavingAnalysis`.

    public inline fun <R> analyze(
        useSiteElement: KtElement,
        action: KaSession.() -> R,
    ): R {
        val analysisSession = getAnalysisSession(useSiteElement)

        beforeEnteringAnalysis(analysisSession, useSiteElement)
        return try {
            analysisSession.action()
        } finally {
            afterLeavingAnalysis(analysisSession, useSiteElement)
        }
    }

    public inline fun <R> analyze(
        useSiteModule: KaModule,
        action: KaSession.() -> R,
    ): R {
        val analysisSession = getAnalysisSession(useSiteModule)

        beforeEnteringAnalysis(analysisSession, useSiteModule)
        return try {
            analysisSession.action()
        } finally {
            afterLeavingAnalysis(analysisSession, useSiteModule)
        }
    }

    /**
     * [beforeEnteringAnalysis] hooks into analysis *before* [analyze]'s action is executed.
     *
     * The signature of [beforeEnteringAnalysis] should be kept stable to avoid breaking binary compatibility, since [analyze] is inlined.
     */
    @KaAnalysisApiInternals
    public abstract fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement)

    /**
     * [beforeEnteringAnalysis] hooks into analysis *before* [analyze]'s action is executed.
     *
     * The signature of [beforeEnteringAnalysis] should be kept stable to avoid breaking binary compatibility, since [analyze] is inlined.
     */
    @KaAnalysisApiInternals
    public abstract fun beforeEnteringAnalysis(session: KaSession, useSiteModule: KaModule)

    /**
     * [afterLeavingAnalysis] hooks into analysis *after* [analyze]'s action has been executed.
     *
     * The signature of [afterLeavingAnalysis] should be kept stable to avoid breaking binary compatibility, since [analyze] is inlined.
     */
    @KaAnalysisApiInternals
    public abstract fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement)

    /**
     * [afterLeavingAnalysis] hooks into analysis *after* [analyze]'s action has been executed.
     *
     * The signature of [afterLeavingAnalysis] should be kept stable to avoid breaking binary compatibility, since [analyze] is inlined.
     */
    @KaAnalysisApiInternals
    public abstract fun afterLeavingAnalysis(session: KaSession, useSiteModule: KaModule)

    @TestOnly
    public abstract fun clearCaches()

    override fun dispose() {}

    public companion object {
        @KaAnalysisApiInternals
        public fun getInstance(project: Project): KaSessionProvider =
            project.getService(KaSessionProvider::class.java)
    }
}

@Deprecated("Use 'KaSessionProvider' instead", ReplaceWith("KaSessionProvider"))
public typealias KtAnalysisSessionProvider = KaSessionProvider
