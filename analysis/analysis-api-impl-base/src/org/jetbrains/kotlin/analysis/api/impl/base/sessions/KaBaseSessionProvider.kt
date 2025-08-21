/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.sessions

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.impl.base.lifetime.KaBaseLifetimeTracker
import org.jetbrains.kotlin.analysis.api.impl.base.permissions.KaBaseWriteActionStartedChecker
import org.jetbrains.kotlin.analysis.api.impl.base.restrictedAnalysis.KaBaseRestrictedAnalysisException
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.KotlinPlatformSettings
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinLifetimeTokenFactory
import org.jetbrains.kotlin.analysis.api.platform.permissions.KaAnalysisPermissionChecker
import org.jetbrains.kotlin.analysis.api.platform.restrictedAnalysis.KotlinRestrictedAnalysisService
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.isResolvable
import org.jetbrains.kotlin.analysis.api.session.KaSessionProvider
import org.jetbrains.kotlin.analysis.api.utils.errors.withKaModuleEntry
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.shouldIjPlatformExceptionBeRethrown

@KaImplementationDetail
abstract class KaBaseSessionProvider(project: Project) : KaSessionProvider(project) {
    // We cache several services to avoid repeated `getService` calls in `analyze`.
    @KaCachedService
    private val permissionChecker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaAnalysisPermissionChecker.getInstance(project)
    }

    @KaCachedService
    private val lifetimeTracker by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KaBaseLifetimeTracker.getInstance(project)
    }

    @KaCachedService
    private val restrictedAnalysisService by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinRestrictedAnalysisService.getInstance(project)
    }

    @KaCachedService
    protected val tokenFactory by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinLifetimeTokenFactory.getInstance(project)
    }

    @KaCachedService
    private val kotlinPlatformSettings by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinPlatformSettings.getInstance(project)
    }

    private val writeActionStartedChecker = KaBaseWriteActionStartedChecker(this)

    protected fun checkUseSiteModule(useSiteModule: KaModule) {
        if (useSiteModule is KaLibraryModule && !kotlinPlatformSettings.allowUseSiteLibraryModuleAnalysis) {
            throw KaBaseUseSiteLibraryModuleAnalysisException(useSiteModule)
        }

        requireWithAttachment(
            useSiteModule.isResolvable,
            { "`${useSiteModule::class.simpleName}` is not resolvable and thus cannot be a use-site module." },
        ) {
            withKaModuleEntry("useSiteModule", useSiteModule)
        }
    }

    override fun beforeEnteringAnalysis(session: KaSession, useSiteElement: KtElement) {
        // Catch issues with analysis on invalid PSI as early as possible.
        PsiUtilCore.ensureValid(useSiteElement)

        beforeEnteringAnalysis(session)
    }

    override fun beforeEnteringAnalysis(session: KaSession, useSiteModule: KaModule) {
        beforeEnteringAnalysis(session)
    }

    private fun beforeEnteringAnalysis(session: KaSession) {
        if (!permissionChecker.isAnalysisAllowed()) {
            throw ProhibitedAnalysisException("Analysis is not allowed: ${permissionChecker.getRejectionReason()}")
        }

        ProgressManager.checkCanceled()

        restrictedAnalysisService?.run {
            if (isAnalysisRestricted && !isRestrictedAnalysisAllowed) {
                rejectRestrictedAnalysis()
            }
        }

        lifetimeTracker.beforeEnteringAnalysis(session)
        writeActionStartedChecker.beforeEnteringAnalysis()
    }

    override fun handleAnalysisException(throwable: Throwable, session: KaSession, useSiteElement: KtElement): Nothing {
        handleAnalysisException(throwable)
    }

    override fun handleAnalysisException(throwable: Throwable, session: KaSession, useSiteModule: KaModule): Nothing {
        handleAnalysisException(throwable)
    }

    private fun handleAnalysisException(throwable: Throwable): Nothing {
        if (
            restrictedAnalysisService?.isAnalysisRestricted == true &&
            throwable !is Error &&
            !shouldIjPlatformExceptionBeRethrown(throwable)
        ) {
            throw KaBaseRestrictedAnalysisException(cause = throwable)
        }

        throw throwable
    }

    override fun afterLeavingAnalysis(session: KaSession, useSiteElement: KtElement) {
        afterLeavingAnalysis(session)
    }

    override fun afterLeavingAnalysis(session: KaSession, useSiteModule: KaModule) {
        afterLeavingAnalysis(session)
    }

    private fun afterLeavingAnalysis(session: KaSession) {
        try {
            // `writeActionStartedChecker` might throw an "illegal write action" exception.
            writeActionStartedChecker.afterLeavingAnalysis()
        } finally {
            lifetimeTracker.afterLeavingAnalysis(session)
        }
    }
}

private class ProhibitedAnalysisException(override val message: String) : IllegalStateException()
