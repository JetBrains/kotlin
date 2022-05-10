/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.tokens

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.session.InvalidWayOfUsingAnalysisSession
import kotlin.reflect.KClass

public class KtReadActionConfinementLifetimeToken(project: Project) : KtLifetimeToken() {
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


    public companion object {
        @HackToForceAllowRunningAnalyzeOnEDT
        public val allowOnEdt: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
    }

    public override val factory: KtLifetimeTokenFactory = ReadActionConfinementValidityTokenFactory
}

public object ReadActionConfinementValidityTokenFactory : KtLifetimeTokenFactory() {
    override val identifier: KClass<out KtLifetimeToken> = KtReadActionConfinementLifetimeToken::class

    override fun create(project: Project): KtLifetimeToken = KtReadActionConfinementLifetimeToken(project)

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
public annotation class HackToForceAllowRunningAnalyzeOnEDT

/**
 * All frontend related work should not be allowed to be ran from EDT thread. Only use it as a temporary solution.
 *
 * @see KtAnalysisSession
 * @see KtReadActionConfinementLifetimeToken
 */
@HackToForceAllowRunningAnalyzeOnEDT
public inline fun <T> hackyAllowRunningOnEdt(action: () -> T): T {
    if (KtReadActionConfinementLifetimeToken.allowOnEdt.get()) return action()
    KtReadActionConfinementLifetimeToken.allowOnEdt.set(true)
    try {
        return action()
    } finally {
        KtReadActionConfinementLifetimeToken.allowOnEdt.set(false)
    }
}