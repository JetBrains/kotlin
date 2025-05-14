/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.platform.KaCachedService
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinAnchorModuleProvider
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEventListener
import org.jetbrains.kotlin.analysis.api.platform.modification.*
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirBuiltinsSessionFactory

/**
 * [LLFirSessionInvalidationService] listens to [modification events][org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent]
 * and invalidates [LLFirSession]s which depend on the modified [KaModule].
 *
 * Its invalidation functions should always be invoked in a **write action** because invalidation affects multiple sessions in
 * [LLFirSessionCache] and the cache has to be kept consistent. The exception is [invalidateAll] – it is called from a stop-the-world
 * cache invalidation (see `KaFirStopWorldCacheCleaner` in Analysis API K2) when it's guaranteed no threads perform code analysis.
 */
@KaImplementationDetail
class LLFirSessionInvalidationService(private val project: Project) {
    internal class LLKotlinModificationEventListener(val project: Project) : KotlinModificationEventListener {
        override fun onModification(event: KotlinModificationEvent) {
            val invalidationService = getInstance(project)
            when (event) {
                is KotlinModuleStateModificationEvent ->
                    when (val module = event.module) {
                        is KaBuiltinsModule -> {
                            // Modification of builtins might affect any session, so all sessions need to be invalidated.
                            invalidationService.invalidateAll(includeLibraryModules = true)
                        }
                        is KaLibraryModule -> {
                            invalidationService.invalidate(module)

                            // A modification to a library module is also a (likely) modification of any fallback dependency module.
                            invalidationService.invalidateFallbackDependencies()
                        }
                        else -> invalidationService.invalidate(module)
                    }

                // We do not need to handle `KaBuiltinsModule` and `KaLibraryModule` here because builtins/libraries cannot be affected by
                // out-of-block modification.
                is KotlinModuleOutOfBlockModificationEvent -> invalidationService.invalidate(event.module)

                is KotlinGlobalModuleStateModificationEvent -> invalidationService.invalidateAll(includeLibraryModules = true)
                is KotlinGlobalSourceModuleStateModificationEvent -> invalidationService.invalidateAll(includeLibraryModules = false)
                is KotlinGlobalScriptModuleStateModificationEvent -> invalidationService.invalidateScriptSessions()
                is KotlinGlobalSourceOutOfBlockModificationEvent -> invalidationService.invalidateAll(includeLibraryModules = false)
                is KotlinCodeFragmentContextModificationEvent -> invalidationService.invalidateContextualDanglingFileSessions(event.module)
            }
        }
    }

    internal class LLPsiModificationTrackerListener(val project: Project) : PsiModificationTracker.Listener {
        override fun modificationCountChanged() {
            getInstance(project).invalidateUnstableDanglingFileSessions()
        }
    }

    @KaCachedService
    private val sessionCache: LLFirSessionCache by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLFirSessionCache.getInstance(project)
    }

    private val invalidator by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLFirSessionCacheStorageInvalidator(sessionCache.storage)
    }

    private val sessionInvalidationEventPublisher: LLFirSessionInvalidationEventPublisher
        get() = LLFirSessionInvalidationEventPublisher.getInstance(project)

    /**
     * Invalidates the session(s) associated with [module].
     *
     * We do not need to handle [KaBuiltinsModule] and invalidate [LLFirBuiltinsSessionFactory] here because we invoke [invalidateAll] when
     * a builtins module receives a targeted modification event.
     *
     * Per the contract of [LLFirSessionInvalidationService], [invalidate] may only be called from a write action.
     */
    private fun invalidate(module: KaModule) = performInvalidation {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        sessionInvalidationEventPublisher.collectSessionsAndPublishInvalidationEvent {
            val didSessionExist = invalidator.removeSession(module)

            // We don't have to invalidate dependent sessions if the root session does not exist in the cache. It is true that sessions can
            // be created without their dependency sessions being created, as session dependencies are lazy. So some of the root session's
            // dependents might exist. But if the root session does not exist, its dependent sessions won't contain any elements resolved by
            // the root session, so they effectively don't depend on the root session at that moment and don't need to be invalidated.
            if (!didSessionExist) return@collectSessionsAndPublishInvalidationEvent

            KotlinModuleDependentsProvider.getInstance(project)
                .getTransitiveDependents(module)
                .forEach(::invalidateDependent)

            // Due to a missing IDE implementation for script dependents (see KTIJ-25620), script sessions need to be invalidated globally:
            //  - A script may include other scripts, so a script modification may affect any other script.
            //  - Script dependencies are also not linked via dependents yet, so any script dependency modification may affect any script.
            //  - Scripts may depend on libraries, and the IDE module dependents provider doesn't provide script dependents for libraries
            //    yet.
            if (module is KaScriptModule || module is KaScriptDependencyModule || module is KaLibraryModule) {
                invalidator.removeAllScriptSessions()
            }

            if (module is KaDanglingFileModule) {
                invalidator.removeContextualDanglingFileSessions(module)
            } else {
                invalidator.removeAllDanglingFileSessions()
            }
        }
    }

    /**
     * Invalidates the session(s) associated with [module]. The module must be a *dependent* of the [KaModule] for which the modification
     * event was received.
     *
     * Dependents have to be handled specially because of a special relationship between a [KaLibraryModule] and its dependencies. Only a
     * resolvable session for a [KaLibraryModule] takes its dependencies into account. A binary session for a library module cannot have any
     * dependencies. Conversely, when we encounter a [KaLibraryModule] as a *dependent*, it can only describe a resolvable session, not a
     * binary session. So it would be inefficient to remove the binary session for the library module from the cache.
     *
     * For example, if the [KaLibraryModule] is a dependent of a [KaLibraryFallbackDependenciesModule], only resolvable library sessions
     * which actually rely on the fallback dependencies should be invalidated.
     */
    private fun invalidateDependent(module: KaModule) {
        if (module is KaLibraryModule) {
            invalidator.removeSourceSession(module)
        } else {
            invalidator.removeSession(module)
        }
    }

    /**
     * Invalidates the sessions of all [KaLibraryFallbackDependenciesModule]s and all dependent resolvable library sessions.
     *
     * When we receive a modification event for a [KaLibraryModule], we have to assume that the content of the library module has been
     * changed in some way. Such a change not only affects the library session, but also all [KaLibraryFallbackDependenciesModule]s, each of
     * which essentially covers (almost) all libraries in the project. So a modification to any library module is also a modification to all
     * fallback dependency modules.
     */
    private fun invalidateFallbackDependencies() = performInvalidation {
        // This solution assumes that only resolvable library sessions can be dependents of fallback dependencies sessions. It would be
        // better to call `invalidate` on each `KaLibraryFallbackDependenciesModule` in the cache ony-by-one as a more general solution.
        // However, this approach is blocked by KT-75688: If we kick off the individual invalidation of fallback modules right now, all
        // source sessions that depend on any fallback module's `dependentLibrary` would be invalidated as well.
        sessionInvalidationEventPublisher.collectSessionsAndPublishInvalidationEvent {
            // Technically, the `KaLibraryFallbackDependenciesModule` which has the modified `KaLibraryModule` as a dependent is *not*
            // affected by a modification to that library module. But there's no large practical advantage in being this selective, so the
            // simpler solution of invalidating all sessions is better.
            invalidator.removeAllLibraryFallbackDependenciesSessions()

            // This is an approximation. Not all resolvable library sessions might depend on fallback dependencies, in which case removing
            // them here is a waste. However, for all practical purposes (especially considering usage in IJ), most of them do, and so this
            // is a sensible simplification.
            invalidator.removeAllResolvableLibrarySessions()
        }
    }

    private fun invalidateScriptSessions() = invalidator.removeAllScriptSessions()

    /**
     * Invalidates all cached sessions. If [includeLibraryModules] is `true`, also invalidates sessions for libraries and builtins.
     *
     * The method must be called in a write action, or alternatively when the caller can guarantee that no other threads can perform
     * invalidation or code analysis until the invalidation is complete.
     */
    fun invalidateAll(includeLibraryModules: Boolean) = performInvalidation {
        if (includeLibraryModules) {
            // Builtins modules and sessions are not part of `LLFirSessionCache`, so they need to be invalidated separately. This can be
            // triggered either by a global module state modification, or a local module state modification of the builtins module itself.
            LLFirBuiltinsSessionFactory.getInstance(project).invalidateAll()
        } else {
            // When anchor modules are configured and `includeLibraryModules` is `false`, we get a situation where the anchor module session
            // will be invalidated (because it is a source session), while its library dependents won't be invalidated. But such library
            // sessions also need to be invalidated because they depend on the anchor module.
            //
            // Invalidating anchor modules before all source sessions has the advantage that `invalidate`'s session existence check will
            // work, so we do not have to invalidate dependent sessions if the anchor module does not exist in the first place.
            val anchorModules = KotlinAnchorModuleProvider.getInstance(project)?.getAllAnchorModulesIfComputed()
            anchorModules?.forEach(::invalidate)
        }

        invalidator.removeAllSessions(includeLibraryModules)

        // We could take `includeLibraryModules` into account here, but this will make the global session invalidation event more
        // complicated to handle, and it isn't currently necessary for `KaFirSession` invalidation to be more granular.
        project.analysisMessageBus.syncPublisher(LLFirSessionInvalidationTopics.SESSION_INVALIDATION).afterGlobalInvalidation()
    }

    private fun invalidateContextualDanglingFileSessions(contextModule: KaModule) = performInvalidation {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        sessionInvalidationEventPublisher.collectSessionsAndPublishInvalidationEvent {
            invalidator.removeContextualDanglingFileSessions(contextModule)
        }
    }

    private fun invalidateUnstableDanglingFileSessions() = performInvalidation {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        // We don't need to publish any session invalidation events for unstable dangling file modules.
        invalidator.removeUnstableDanglingFileSessions()
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
    private inline fun performInvalidation(block: () -> Unit) {
        synchronized(this) {
            block()
        }
    }

    companion object {
        fun getInstance(project: Project): LLFirSessionInvalidationService =
            project.getService(LLFirSessionInvalidationService::class.java)
    }
}
