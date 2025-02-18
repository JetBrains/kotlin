/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinAnchorModuleProvider
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinCodeFragmentContextModificationListener
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModuleStateModificationListener
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalSourceModuleStateModificationListener
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalSourceOutOfBlockModificationListener
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleOutOfBlockModificationListener
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleStateModificationKind
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleStateModificationListener
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalScriptModuleStateModificationListener
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptDependencyModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule

/**
 * [LLFirSessionInvalidationService] listens to [modification events][org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationTopics]
 * and invalidates [LLFirSession]s which depend on the modified [KaModule].
 *
 * Its invalidation functions should always be invoked in a **write action** because invalidation affects multiple sessions in
 * [LLFirSessionCache] and the cache has to be kept consistent. The exception is [invalidateAll] – it is called from a stop-the-world
 * cache invalidation (see `KaFirStopWorldCacheCleaner` in Analysis API K2) when it's guaranteed no threads perform code analysis.
 */
@KaImplementationDetail
class LLFirSessionInvalidationService(val project: Project) {
    class LLKotlinModuleStateModificationListener(val project: Project) : KotlinModuleStateModificationListener {
        override fun onModification(module: KaModule, modificationKind: KotlinModuleStateModificationKind) {
            getInstance(project).invalidate(module)
        }
    }

    class LLKotlinModuleOutOfBlockModificationListener(val project: Project) : KotlinModuleOutOfBlockModificationListener {
        override fun onModification(module: KaModule) {
            getInstance(project).invalidate(module)
        }
    }

    class LLKotlinGlobalModuleStateModificationListener(val project: Project) : KotlinGlobalModuleStateModificationListener {
        override fun onModification() {
            getInstance(project).invalidateAll(includeLibraryModules = true)
        }
    }

    class LLKotlinGlobalSourceModuleStateModificationListener(val project: Project) :
        KotlinGlobalSourceModuleStateModificationListener {
        override fun onModification() {
            getInstance(project).invalidateAll(includeLibraryModules = false)
        }
    }

    class LLKotlinGlobalSourceOutOfBlockModificationListener(val project: Project) :
        KotlinGlobalSourceOutOfBlockModificationListener {
        override fun onModification() {
            getInstance(project).invalidateAll(includeLibraryModules = false)
        }
    }

    class LLKotlinCodeFragmentContextModificationListener(val project: Project) : KotlinCodeFragmentContextModificationListener {
        override fun onModification(module: KaModule) {
            getInstance(project).invalidateContextualDanglingFileSessions(module)
        }
    }

    class LLPsiModificationTrackerListener(val project: Project) : PsiModificationTracker.Listener {
        override fun modificationCountChanged() {
            getInstance(project).invalidateUnstableDanglingFileSessions()
        }
    }

    class LLKotlinGlobalScriptModuleStateModificationListener(val project: Project) :
        KotlinGlobalScriptModuleStateModificationListener {
        override fun onModification() {
            getInstance(project).invalidateScriptSessions()
        }
    }

    val sessionCache: LLFirSessionCache by lazy {
        LLFirSessionCache.getInstance(project)
    }

    val sessionInvalidationEventPublisher: LLFirSessionInvalidationEventPublisher
        get() = LLFirSessionInvalidationEventPublisher.getInstance(project)

    /**
     * Invalidates the session(s) associated with [module].
     *
     * Per the contract of [LLFirSessionInvalidationService], [invalidate] may only be called from a write action.
     */
    fun invalidate(module: KaModule) = performInvalidation {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        sessionInvalidationEventPublisher.collectSessionsAndPublishInvalidationEvent {
            val didSessionExist = sessionCache.removeSession(module)

            // We don't have to invalidate dependent sessions if the root session does not exist in the cache. It is true that sessions can
            // be created without their dependency sessions being created, as session dependencies are lazy. So some of the root session's
            // dependents might exist. But if the root session does not exist, its dependent sessions won't contain any elements resolved by
            // the root session, so they effectively don't depend on the root session at that moment and don't need to be invalidated.
            if (!didSessionExist) return@collectSessionsAndPublishInvalidationEvent

            KotlinModuleDependentsProvider.getInstance(project).getTransitiveDependents(module).forEach(sessionCache::removeSession)

            // Due to a missing IDE implementation for script dependents (see KTIJ-25620), script sessions need to be invalidated globally:
            //  - A script may include other scripts, so a script modification may affect any other script.
            //  - Script dependencies are also not linked via dependents yet, so any script dependency modification may affect any script.
            //  - Scripts may depend on libraries, and the IDE module dependents provider doesn't provide script dependents for libraries
            //    yet.
            if (module is KaScriptModule || module is KaScriptDependencyModule || module is KaLibraryModule) {
                sessionCache.removeAllScriptSessions()
            }

            if (module is KaDanglingFileModule) {
                sessionCache.removeContextualDanglingFileSessions(module)
            } else {
                sessionCache.removeAllDanglingFileSessions()
            }
        }
    }

    fun invalidateScriptSessions() = sessionCache.removeAllScriptSessions()

    /**
     * Invalidates all cached sessions. If [includeLibraryModules] is `true`, also invalidates sessions for libraries.
     *
     * The method must be called in a write action, or in the case if the caller can guarantee no other threads can perform invalidation or
     * code analysis until the invalidation is complete.
     */
    fun invalidateAll(includeLibraryModules: Boolean) = performInvalidation {
        // When anchor modules are configured and `includeLibraryModules` is `false`, we get a situation where the anchor module session
        // will be invalidated (because it is a source session), while its library dependents won't be invalidated. But such library
        // sessions also need to be invalidated because they depend on the anchor module.
        //
        // Invalidating anchor modules before all source sessions has the advantage that `invalidate`'s session existence check will work,
        // so we do not have to invalidate dependent sessions if the anchor module does not exist in the first place.
        if (!includeLibraryModules) {
            val anchorModules = KotlinAnchorModuleProvider.getInstance(project)?.getAllAnchorModulesIfComputed()
            anchorModules?.forEach(::invalidate)
        }

        sessionCache.removeAllSessions(includeLibraryModules)

        // We could take `includeLibraryModules` into account here, but this will make the global session invalidation event more
        // complicated to handle, and it isn't currently necessary for `KaFirSession` invalidation to be more granular.
        project.analysisMessageBus.syncPublisher(LLFirSessionInvalidationTopics.SESSION_INVALIDATION).afterGlobalInvalidation()
    }

    fun invalidateContextualDanglingFileSessions(contextModule: KaModule) = performInvalidation {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        sessionInvalidationEventPublisher.collectSessionsAndPublishInvalidationEvent {
            sessionCache.removeContextualDanglingFileSessions(contextModule)
        }
    }

    fun invalidateUnstableDanglingFileSessions() = performInvalidation {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        // We don't need to publish any session invalidation events for unstable dangling file modules.
        sessionCache.removeUnstableDanglingFileSessions()
    }

    /**
     * Ensures that the invalidated [block] does not run concurrently.
     *
     * Invalidation caused by a listener only happens in a write action. However, `KaFirCacheCleaner` can perform immediate invalidation
     * from outside one if there are no ongoing analyses. There might happen a situation when both an invalidation listener and the
     * stop-world cache cleaner request invalidation at once.
     *
     * The synchronization protects concurrent-unsafe cleanup in [SessionStorage].
     */
    inline fun performInvalidation(block: () -> Unit) {
        synchronized(this) {
            block()
        }
    }

    companion object {
        fun getInstance(project: Project): LLFirSessionInvalidationService =
            project.getService(LLFirSessionInvalidationService::class.java)
    }
}
