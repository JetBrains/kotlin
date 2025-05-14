/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventListener
import org.jetbrains.kotlin.analysis.api.platform.modification.*
import org.jetbrains.kotlin.analysis.api.projectStructure.*

/**
 * [LLFirSessionInvalidationService] listens to [modification events][org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent]
 * and invalidates [LLFirSession]s which depend on the modified [KaModule].
 */
@KaImplementationDetail
class LLFirSessionInvalidationService(private val project: Project) {
    internal class LLKotlinModificationEventListener(val project: Project) : KotlinModificationEventListener {
        override fun onModification(event: KotlinModificationEvent) {
            val invalidator = getInstance(project).invalidator
            when (event) {
                is KotlinModuleStateModificationEvent ->
                    when (val module = event.module) {
                        is KaBuiltinsModule -> {
                            // Modification of builtins might affect any session, so all sessions need to be invalidated.
                            invalidator.invalidateAll(includeLibraryModules = true)
                        }
                        is KaLibraryModule -> {
                            invalidator.invalidate(module)

                            // A modification to a library module is also a (likely) modification of any fallback dependency module.
                            invalidator.invalidateFallbackDependencies()
                        }
                        else -> invalidator.invalidate(module)
                    }

                // We do not need to handle `KaBuiltinsModule` and `KaLibraryModule` here because builtins/libraries cannot be affected by
                // out-of-block modification.
                is KotlinModuleOutOfBlockModificationEvent -> invalidator.invalidate(event.module)

                is KotlinGlobalModuleStateModificationEvent -> invalidator.invalidateAll(includeLibraryModules = true)
                is KotlinGlobalSourceModuleStateModificationEvent -> invalidator.invalidateAll(includeLibraryModules = false)
                is KotlinGlobalScriptModuleStateModificationEvent -> invalidator.invalidateScriptSessions()
                is KotlinGlobalSourceOutOfBlockModificationEvent -> invalidator.invalidateAll(includeLibraryModules = false)
                is KotlinCodeFragmentContextModificationEvent -> invalidator.invalidateContextualDanglingFileSessions(event.module)
            }
        }
    }

    internal class LLPsiModificationTrackerListener(val project: Project) : PsiModificationTracker.Listener {
        override fun modificationCountChanged() {
            getInstance(project).invalidator.invalidateUnstableDanglingFileSessions()
        }
    }

    @KaCachedService
    private val sessionCache: LLFirSessionCache by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLFirSessionCache.getInstance(project)
    }

    private val invalidator by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLFirSessionCacheStorageInvalidator(project, sessionCache.storage)
    }

    /**
     * @see LLFirSessionCacheStorageInvalidator.invalidateAll
     */
    fun invalidateAll(includeLibraryModules: Boolean) {
        invalidator.invalidateAll(includeLibraryModules)
    }

    companion object {
        fun getInstance(project: Project): LLFirSessionInvalidationService =
            project.getService(LLFirSessionInvalidationService::class.java)
    }
}
