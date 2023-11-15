/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.KotlinAnchorModuleProvider
import org.jetbrains.kotlin.analysis.providers.analysisMessageBus
import org.jetbrains.kotlin.analysis.providers.topics.*

/**
 * [LLFirSessionInvalidationService] listens to [modification events][KotlinTopics] and invalidates [LLFirSession]s which depend on the
 * modified [KtModule]. Its invalidation functions should always be invoked in a **write action** because invalidation affects multiple
 * sessions in [LLFirSessionCache] and the cache has to be kept consistent.
 */
class LLFirSessionInvalidationService(private val project: Project) : Disposable {
    private val sessionCache: LLFirSessionCache by lazy {
        LLFirSessionCache.getInstance(project)
    }

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
            KotlinGlobalModuleStateModificationListener { invalidateAll(includeLibraryModules = true) }
        )
        busConnection.subscribe(
            KotlinTopics.GLOBAL_SOURCE_MODULE_STATE_MODIFICATION,
            KotlinGlobalSourceModuleStateModificationListener { invalidateAll(includeLibraryModules = false) },
        )
        busConnection.subscribe(
            KotlinTopics.GLOBAL_SOURCE_OUT_OF_BLOCK_MODIFICATION,
            KotlinGlobalSourceOutOfBlockModificationListener { invalidateAll(includeLibraryModules = false) },
        )
        busConnection.subscribe(
            KotlinTopics.CODE_FRAGMENT_CONTEXT_MODIFICATION,
            KotlinCodeFragmentContextModificationListener { module -> invalidateContextualDanglingFileSessions(module) }
        )
        busConnection.subscribe(
            PsiModificationTracker.TOPIC,
            PsiModificationTracker.Listener { invalidateUnstableDanglingFileSessions() }
        )
    }

    /**
     * Invalidates the session(s) associated with [module].
     *
     * Per the contract of [LLFirSessionInvalidationService], [invalidate] may only be called from a write action.
     */
    fun invalidate(module: KtModule) {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        val didSessionExist = sessionCache.removeSession(module)

        // We don't have to invalidate dependent sessions if the root session does not exist in the cache. It is true that sessions can be
        // created without their dependency sessions being created, as session dependencies are lazy. So some of the root session's
        // dependents might exist. But if the root session does not exist, its dependent sessions won't contain any elements resolved by the
        // root session, so they effectively don't depend on the root session at that moment and don't need to be invalidated.
        if (!didSessionExist) return

        KotlinModuleDependentsProvider.getInstance(project).getTransitiveDependents(module).forEach(sessionCache::removeSession)

        // Due to a missing IDE implementation for script dependents (see KTIJ-25620), script sessions need to be invalidated globally:
        //  - A script may include other scripts, so a script modification may affect any other script.
        //  - Script dependencies are also not linked via dependents yet, so any script dependency modification may affect any script.
        //  - Scripts may depend on libraries, and the IDE module dependents provider doesn't provide script dependents for libraries yet.
        if (module is KtScriptModule || module is KtScriptDependencyModule || module is KtLibraryModule) {
            sessionCache.removeAllScriptSessions()
        }

        if (module is KtDanglingFileModule) {
            sessionCache.removeContextualDanglingFileSessions(module)
        } else {
            sessionCache.removeAllDanglingFileSessions()
        }
    }

    private fun invalidateAll(includeLibraryModules: Boolean) {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        // When anchor modules are configured and `includeLibraryModules` is `false`, we get a situation where the anchor module session
        // will be invalidated (because it is a source session), while its library dependents won't be invalidated. But such library
        // sessions also need to be invalidated because they depend on the anchor module.
        //
        // Invalidating anchor modules before all source sessions has the advantage that `invalidate`'s session existence check will work,
        // so we do not have to invalidate dependent sessions if the anchor module does not exist in the first place.
        if (!includeLibraryModules) {
            val anchorModules = KotlinAnchorModuleProvider.getInstance(project)?.getAllAnchorModules()
            anchorModules?.forEach(::invalidate)
        }

        sessionCache.removeAllSessions(includeLibraryModules)
    }

    private fun invalidateContextualDanglingFileSessions(contextModule: KtModule) {
        sessionCache.removeContextualDanglingFileSessions(contextModule)
    }

    private fun invalidateUnstableDanglingFileSessions() {
        sessionCache.removeUnstableDanglingFileSessions()
    }

    override fun dispose() {
    }

    companion object {
        fun getInstance(project: Project): LLFirSessionInvalidationService =
            project.getService(LLFirSessionInvalidationService::class.java)
    }
}
