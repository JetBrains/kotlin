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
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.psi.KtElement

/**
 * Provides [KaSession]s by use-site [KtElement]s or [KtModule]s.
 *
 * This provider should not be used directly.
 * Please use [analyze][org.jetbrains.kotlin.analysis.api.analyze] or [analyzeCopy][org.jetbrains.kotlin.analysis.api.analyzeCopy] instead.
 */
@OptIn(KaAnalysisApiInternals::class)
public abstract class KaSessionProvider(public val project: Project) : Disposable {
    public abstract val tokenFactory: KaLifetimeTokenFactory

    public abstract fun getAnalysisSession(useSiteKtElement: KtElement): KaSession

    public abstract fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule): KaSession

    // The `analyse` functions affect binary compatibility as they are inlined with every `analyze` call. To avoid breaking binary
    // compatibility, their implementations should not be changed unless absolutely necessary. It should be possible to put most
    // functionality into `beforeEnteringAnalysis` and/or `afterLeavingAnalysis`.

    public inline fun <R> analyze(
        useSiteKtElement: KtElement,
        action: KaSession.() -> R,
    ): R {
        val analysisSession = getAnalysisSession(useSiteKtElement)

        beforeEnteringAnalysis(analysisSession, useSiteKtElement)
        return try {
            analysisSession.action()
        } finally {
            afterLeavingAnalysis(analysisSession, useSiteKtElement)
        }
    }

    public inline fun <R> analyze(
        useSiteKtModule: KtModule,
        action: KaSession.() -> R,
    ): R {
        val analysisSession = getAnalysisSessionByUseSiteKtModule(useSiteKtModule)

        beforeEnteringAnalysis(analysisSession, useSiteKtModule)
        return try {
            analysisSession.action()
        } finally {
            afterLeavingAnalysis(analysisSession, useSiteKtModule)
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
    public abstract fun beforeEnteringAnalysis(session: KaSession, useSiteModule: KtModule)

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
    public abstract fun afterLeavingAnalysis(session: KaSession, useSiteModule: KtModule)

    @TestOnly
    public abstract fun clearCaches()

    override fun dispose() {}

    public companion object {
        @KaAnalysisApiInternals
        public fun getInstance(project: Project): KaSessionProvider =
            project.getService(KaSessionProvider::class.java)
    }
}

public typealias KtAnalysisSessionProvider = KaSessionProvider
