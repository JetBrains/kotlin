/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.project.structure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus
import org.jetbrains.kotlin.analysis.providers.topics.KotlinGlobalModuleStateModificationListener
import org.jetbrains.kotlin.analysis.providers.topics.KotlinGlobalOutOfBlockModificationListener
import org.jetbrains.kotlin.analysis.providers.topics.KotlinGlobalSourceModuleStateModificationListener
import org.jetbrains.kotlin.analysis.providers.topics.KotlinGlobalSourceOutOfBlockModificationListener
import org.jetbrains.kotlin.analysis.providers.topics.KotlinTopics
import org.jetbrains.kotlin.analysis.providers.topics.KotlinModuleOutOfBlockModificationListener
import org.jetbrains.kotlin.analysis.providers.topics.KotlinModuleStateModificationListener

/**
 * [LLFirSessionInvalidationService] listens to [modification events][KotlinTopics] and invalidates [LLFirSession]s which depend on the
 * modified [KtModule]. Its invalidation functions should always be invoked in a **write action** because invalidation affects multiple
 * sessions in [LLFirSessionCache] and the cache has to be kept consistent.
 */
class LLFirSessionInvalidationService(private val project: Project) : Disposable {
    /**
     * Subscribes to all [modification events][KotlinTopics] via the [analysisMessageBus].
     *
     * [subscribeToModificationEvents] must be invoked during setup to allow [LLFirSessionInvalidationService] to listen to events.
     * Subscribing in `init` is not an option because services are created on demand and there is no guarantee that this service is going to
     * be requested.
     */
    fun subscribeToModificationEvents() {
        val busConnection = project.analysisMessageBus.connect(this)

        // All modification events the invalidation service subscribes to are guaranteed to be published in a write action. This ensures
        // that invalidation functions are only called in a write action, per the contract of `LLFirSessionInvalidationService`.
        busConnection.subscribe(
            KotlinTopics.MODULE_STATE_MODIFICATION,
            KotlinModuleStateModificationListener { module, _ -> invalidate(module) },
        )
        busConnection.subscribe(
            KotlinTopics.MODULE_OUT_OF_BLOCK_MODIFICATION,
            KotlinModuleOutOfBlockModificationListener { module -> invalidate(module) },
        )
        busConnection.subscribe(
            KotlinTopics.GLOBAL_MODULE_STATE_MODIFICATION,
            KotlinGlobalModuleStateModificationListener { invalidateAll(includeBinaryModules = true) }
        )
        busConnection.subscribe(
            KotlinTopics.GLOBAL_OUT_OF_BLOCK_MODIFICATION,
            KotlinGlobalOutOfBlockModificationListener { invalidateAll(includeBinaryModules = true) }
        )
        busConnection.subscribe(
            KotlinTopics.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
            KotlinGlobalSourceModuleStateModificationListener { invalidateAll(includeBinaryModules = false) },
        )
        busConnection.subscribe(
            KotlinTopics.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION,
            KotlinGlobalSourceOutOfBlockModificationListener { invalidateAll(includeBinaryModules = false) },
        )
    }

    /**
     * Invalidates the session(s) associated with [module].
     *
     * Per the contract of [LLFirSessionInvalidationService], [invalidate] may only be called from a write action.
     */
    fun invalidate(module: KtModule) {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        val sessionCache = LLFirSessionCache.getInstance(project)
        sessionCache.removeSession(module)
        KotlinModuleDependentsProvider.getInstance(project).getTransitiveDependents(module).forEach(sessionCache::removeSession)
    }

    private fun invalidateAll(includeBinaryModules: Boolean) {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        LLFirSessionCache.getInstance(project).removeAllSessions(includeBinaryModules)
    }

    override fun dispose() {
    }

    companion object {
        fun getInstance(project: Project): LLFirSessionInvalidationService =
            project.getService(LLFirSessionInvalidationService::class.java)
    }
}
