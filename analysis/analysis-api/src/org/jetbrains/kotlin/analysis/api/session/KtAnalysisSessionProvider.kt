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
import org.jetbrains.kotlin.analysis.api.lifetime.impl.NoWriteActionInAnalyseCallChecker
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeTokenProvider
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
    @KaAnalysisApiInternals
    public val tokenFactory: KaLifetimeTokenFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaLifetimeTokenProvider.getService(project).getLifetimeTokenFactory()
    }

    @Suppress("LeakingThis")
    public val noWriteActionInAnalyseCallChecker: NoWriteActionInAnalyseCallChecker = NoWriteActionInAnalyseCallChecker(this)

    public abstract fun getAnalysisSession(useSiteKtElement: KtElement): KaSession

    public abstract fun getAnalysisSessionByUseSiteKtModule(useSiteKtModule: KtModule): KaSession

    public inline fun <R> analyse(
        useSiteKtElement: KtElement,
        action: KaSession.() -> R,
    ): R {
        return analyse(getAnalysisSession(useSiteKtElement), action)
    }

    public inline fun <R> analyze(
        useSiteKtModule: KtModule,
        action: KaSession.() -> R,
    ): R {
        return analyse(getAnalysisSessionByUseSiteKtModule(useSiteKtModule), action)
    }

    public inline fun <R> analyse(
        analysisSession: KaSession,
        action: KaSession.() -> R,
    ): R {
        noWriteActionInAnalyseCallChecker.beforeEnteringAnalysisContext()
        tokenFactory.beforeEnteringAnalysisContext(analysisSession.token)
        return try {
            analysisSession.action()
        } finally {
            tokenFactory.afterLeavingAnalysisContext(analysisSession.token)
            noWriteActionInAnalyseCallChecker.afterLeavingAnalysisContext()
        }
    }

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