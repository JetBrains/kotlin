/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.psi.KtElement

/**
 * Provides [KaSession]s by use-site [KtElement]s or [KaModule]s.
 *
 * This provider should not be used directly.
 * Please use [analyze][org.jetbrains.kotlin.analysis.api.analyze] or [analyzeCopy][org.jetbrains.kotlin.analysis.api.analyzeCopy] instead.
 */
@KaImplementationDetail
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
            val lock = Any()
            synchronized(lock) {
                // A 'synchronized' block here prevents non-local suspend calls from being inlined.
                // There will be a compilation error if a suspend function is called from the inside.
                analysisSession.action()
            }
        } catch (throwable: Throwable) {
            handleAnalysisException(throwable, analysisSession, useSiteElement)
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
            val lock = Any()
            synchronized(lock) {
                // A 'synchronized' block here prevents non-local suspend calls from being inlined.
                // There will be a compilation error if a suspend function is called from the inside.
                analysisSession.action()
            }
        } catch (throwable: Throwable) {
            handleAnalysisException(throwable, analysisSession, useSiteModule)
        } finally {
            afterLeavingAnalysis(analysisSession, useSiteModule)
        }
    }

    /**
     * [beforeEnteringAnalysis] hooks into analysis *before* [analyze]'s action is executed.
     *
     * The signature of [beforeEnteringAnalysis] should be kept stable to avoid breaking binary compatibility, since [analyze] is inlined.
     */
    @KaImplementationDetail
    public abstract fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement)

    /**
     * This function has the same contracts as [beforeEnteringAnalysis] for [KtElement]s.
     */
    @KaImplementationDetail
    public abstract fun beforeEnteringAnalysis(session: KaSession, useSiteModule: KaModule)

    /**
     * [handleAnalysisException] handles any [Throwable] that occurred during analysis and was caught by [analyze].
     *
     * [Error]s should generally be rethrown. That said, the signature includes [Throwable] and not [Exception] to stay as wide as possible
     * to avoid risking breaking binary compatibility down the line.
     *
     * The signature of [handleAnalysisException] should be kept stable to avoid breaking binary compatibility, since [analyze] is inlined.
     */
    @KaImplementationDetail
    public abstract fun handleAnalysisException(throwable: Throwable, session: KaSession, useSiteElement: KtElement): Nothing

    /**
     * This function has the same contracts as [handleAnalysisException] for [KtElement]s.
     */
    @KaImplementationDetail
    public abstract fun handleAnalysisException(throwable: Throwable, session: KaSession, useSiteModule: KaModule): Nothing

    /**
     * [afterLeavingAnalysis] hooks into analysis *after* [analyze]'s action has been executed.
     *
     * The signature of [afterLeavingAnalysis] should be kept stable to avoid breaking binary compatibility, since [analyze] is inlined.
     */
    @KaImplementationDetail
    public abstract fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement)

    /**
     * This function has the same contracts as [afterLeavingAnalysis] for [KtElement]s.
     */
    @KaImplementationDetail
    public abstract fun afterLeavingAnalysis(session: KaSession, useSiteModule: KaModule)

    @KaImplementationDetail
    public abstract fun clearCaches()

    override fun dispose() {}

    @KaImplementationDetail
    public companion object {
        @KaImplementationDetail
        public fun getInstance(project: Project): KaSessionProvider =
            project.getService(KaSessionProvider::class.java)
    }
}
