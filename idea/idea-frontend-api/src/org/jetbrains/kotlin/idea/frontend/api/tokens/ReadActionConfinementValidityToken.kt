/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.tokens

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.trackers.createProjectWideOutOfBlockModificationTracker
import kotlin.reflect.KClass

class ReadActionConfinementValidityToken(project: Project) : ValidityToken() {
    private val modificationTracker = project.createProjectWideOutOfBlockModificationTracker()
    private val onCreatedTimeStamp = modificationTracker.modificationCount

    override fun isValid(): Boolean {
        return onCreatedTimeStamp == modificationTracker.modificationCount
    }

    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    override fun getInvalidationReason(): String {
        if (onCreatedTimeStamp != modificationTracker.modificationCount) return "PSI has changed since creation"
        error("Getting invalidation reason for valid validity token")
    }

    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class, ForbidKtResolveInternals::class, InvalidWayOfUsingAnalysisSession::class)
    override fun isAccessible(): Boolean {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread && !allowOnEdt.get()) return false
        if (ForbidKtResolve.resovleIsForbidenInActionWithName.get() != null) return false
        if (!application.isReadAccessAllowed) return false
        if (!ReadActionConfinementValidityTokenFactory.isInsideAnalysisContext()) return false
        return true
    }

    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class, ForbidKtResolveInternals::class, InvalidWayOfUsingAnalysisSession::class)
    override fun getInaccessibilityReason(): String {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread && !allowOnEdt.get()) return "Called in EDT thread"
        if (!application.isReadAccessAllowed) return "Called outside read action"
        ForbidKtResolve.resovleIsForbidenInActionWithName.get()?.let { actionName ->
            return "Resolve is forbidden in $actionName"
        }
        if (!ReadActionConfinementValidityTokenFactory.isInsideAnalysisContext()) return "Called outside analyse method"
        error("Getting inaccessibility reason for validity token when it is accessible")
    }


    companion object {
        @HackToForceAllowRunningAnalyzeOnEDT
        val allowOnEdt: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    }
}

object ReadActionConfinementValidityTokenFactory : ValidityTokenFactory() {
    override val identifier: KClass<out ValidityToken> = ReadActionConfinementValidityToken::class

    override fun create(project: Project): ValidityToken = ReadActionConfinementValidityToken(project)

    override fun beforeEnteringAnalysisContext() {
        currentAnalysisContextEnteringCount.set(currentAnalysisContextEnteringCount.get() + 1)
    }

    override fun afterLeavingAnalysisContext() {
        currentAnalysisContextEnteringCount.set(currentAnalysisContextEnteringCount.get() - 1)
    }

    private val currentAnalysisContextEnteringCount = ThreadLocal.withInitial { 0 }

    internal fun isInsideAnalysisContext() = currentAnalysisContextEnteringCount.get() > 0
}

@RequiresOptIn("All frontend related work should not be allowed to be ran from EDT thread. Only use it as a temporary solution")
annotation class HackToForceAllowRunningAnalyzeOnEDT

/**
 * All frontend related work should not be allowed to be ran from EDT thread. Only use it as a temporary solution.
 *
 * @see KtAnalysisSession
 * @see ReadActionConfinementValidityToken
 */
@HackToForceAllowRunningAnalyzeOnEDT
inline fun <T> hackyAllowRunningOnEdt(action: () -> T): T {
    if (ReadActionConfinementValidityToken.allowOnEdt.get()) return action()
    ReadActionConfinementValidityToken.allowOnEdt.set(true)
    try {
        return action()
    } finally {
        ReadActionConfinementValidityToken.allowOnEdt.set(false)
    }
}