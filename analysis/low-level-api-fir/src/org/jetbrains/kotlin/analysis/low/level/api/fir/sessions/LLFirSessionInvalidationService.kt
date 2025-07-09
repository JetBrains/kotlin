/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventListener
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule

/**
 * [LLFirSessionInvalidationService] listens to [modification events][org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent]
 * and invalidates [LLFirSession]s which depend on the modified [KaModule].
 */
@KaImplementationDetail
class LLFirSessionInvalidationService(private val project: Project) {
    internal class LLKotlinModificationEventListener(val project: Project) : KotlinModificationEventListener {
        override fun onModification(event: KotlinModificationEvent) {
            getInstance(project).invalidate(event)
        }
    }

    internal class LLPsiModificationTrackerListener(val project: Project) : PsiModificationTracker.Listener {
        override fun modificationCountChanged() {
            getInstance(project).invalidator.invalidateUnstableDanglingFileSessions()
        }
    }

    private val invalidator by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLFirSessionCacheStorageInvalidator(project, LLFirSessionCache.getInstance(project).storage)
    }

    /**
     * @see LLFirSessionCacheStorageInvalidator.invalidate
     */
    fun invalidate(event: KotlinModificationEvent) {
        invalidator.invalidate(event)
    }

    /**
     * @see LLFirSessionCacheStorageInvalidator.invalidateAll
     */
    fun invalidateAll(includeLibraryModules: Boolean, diagnosticInformation: String? = null) {
        invalidator.invalidateAll(includeLibraryModules, diagnosticInformation)
    }

    companion object {
        fun getInstance(project: Project): LLFirSessionInvalidationService =
            project.getService(LLFirSessionInvalidationService::class.java)
    }
}
