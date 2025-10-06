/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.platform.analysisMessageBus
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinCodeFragmentContextModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalScriptModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalSourceModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinGlobalSourceOutOfBlockModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleOutOfBlockModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.modification.KotlinModuleStateModificationEvent
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinAnchorModuleProvider
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinModuleDependentsProvider
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure.LLFirBuiltinsSessionFactory

/**
 * Invalidates sessions in [LLFirSessionCacheStorage] when the corresponding module state has changed.
 *
 * Its invalidation functions should always be invoked in a **write action** because invalidation affects multiple sessions in
 * [LLFirSessionCache] and the cache has to be kept consistent. The exception is [invalidateAll] – it is called from a stop-the-world
 * cache invalidation (see `KaFirStopWorldCacheCleaner` in Analysis API K2) when it's guaranteed no threads perform code analysis.
 *
 * ### Cache consistency
 *
 * Invalidation must also preserve cache consistency. A cache is consistent when none of the sessions are outdated and all sessions
 * referenced by other sessions also exist in the cache. Ensuring this is straightforward on the happy path but gets more complicated when
 * exceptions are involved.
 *
 * To achieve resilience, the invalidator takes the following measures:
 *
 * - All invalidation is performed in non-cancellable sections, which avoids (most) cancellation exceptions.
 * - When removing entries from [LLFirSessionCacheStorage] caches, session cleanup with [LLFirSessionCleaner] might cause exceptions. The
 *   cache implementation ensures that if such an exception happens, removal from the cache is still guaranteed.
 * - An invalidation operation might be composed of several, independent cache removal operations. The invalidator uses
 *   [withIndependentRemoval] to ensure that all removal operations run even if one of them throws an exception.
 */
@LLFirInternals
class LLFirSessionCacheStorageInvalidator(
    private val project: Project,
    private val storage: LLFirSessionCacheStorage,
) {
    private val sessionInvalidationEventPublisher: LLFirSessionInvalidationEventPublisher
        get() = LLFirSessionInvalidationEventPublisher.getInstance(project)

    fun invalidate(event: KotlinModificationEvent) {
        when (event) {
            is KotlinModuleStateModificationEvent ->
                when (val module = event.module) {
                    is KaBuiltinsModule -> {
                        // Modification of fallback builtins might affect any session, so all sessions need to be invalidated.
                        invalidateAll(includeLibraryModules = true, "builtins module state modification")
                    }
                    is KaLibraryModule -> {
                        invalidate(module)

                        // A modification to a library module is also a (likely) modification of any fallback dependency module.
                        invalidateFallbackDependencies()
                    }
                    else -> invalidate(module)
                }

            // We do not need to handle `KaBuiltinsModule` and `KaLibraryModule` here because builtins/libraries cannot be affected by
            // out-of-block modification.
            is KotlinModuleOutOfBlockModificationEvent -> invalidate(event.module)

            is KotlinGlobalModuleStateModificationEvent -> invalidateAll(includeLibraryModules = true, "global module state modification")

            is KotlinGlobalSourceModuleStateModificationEvent ->
                invalidateAll(includeLibraryModules = false, "source module state modification")

            is KotlinGlobalScriptModuleStateModificationEvent -> invalidateScriptSessions()

            is KotlinGlobalSourceOutOfBlockModificationEvent ->
                invalidateAll(includeLibraryModules = false, "global source out-of-block modification")

            is KotlinCodeFragmentContextModificationEvent -> invalidateContextualDanglingFileSessions(event.module)
        }
    }

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
            val didSessionExist = removeSession(module)

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
                removeAllScriptSessions()
            }

            if (module is KaDanglingFileModule) {
                removeContextualDanglingFileSessions(module)
            } else {
                removeAllDanglingFileSessions()
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
            removeSourceSession(module)
        } else {
            removeSession(module)
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
            removeAllLibraryFallbackDependenciesSessions()

            // This is an approximation. Not all resolvable library sessions might depend on fallback dependencies, in which case removing
            // them here is a waste. However, for all practical purposes (especially considering usage in IJ), most of them do, and so this
            // is a sensible simplification.
            removeAllResolvableLibrarySessions()
        }
    }

    private fun invalidateScriptSessions() = performInvalidation { removeAllScriptSessions() }

    /**
     * Invalidates all cached sessions. If [includeLibraryModules] is `true`, also invalidates sessions for libraries and builtins.
     *
     * The method must be called in a write action, or alternatively when the caller can guarantee that no other threads can perform
     * invalidation or code analysis until the invalidation is complete.
     */
    fun invalidateAll(includeLibraryModules: Boolean, diagnosticInformation: String? = null) = performInvalidation {
        try {
            withIndependentRemoval {
                if (includeLibraryModules) {
                    // Builtins modules and sessions are not part of `LLFirSessionCache`, so they need to be invalidated separately. This
                    // can be triggered either by a global module state modification or a local module state modification of the builtins
                    // module itself.
                    independently { LLFirBuiltinsSessionFactory.getInstance(project).invalidateAll() }
                } else {
                    // When anchor modules are configured and `includeLibraryModules` is `false`, we get a situation where the anchor module session
                    // will be invalidated (because it is a source session), while its library dependents won't be invalidated. But such library
                    // sessions also need to be invalidated because they depend on the anchor module.
                    //
                    // Invalidating anchor modules before all source sessions has the advantage that `invalidate`'s session existence check will
                    // work, so we do not have to invalidate dependent sessions if the anchor module does not exist in the first place.
                    val anchorModules = KotlinAnchorModuleProvider.getInstance(project)?.getAllAnchorModulesIfComputed()
                    anchorModules?.forEach { anchorModule ->
                        independently { invalidate(anchorModule) }
                    }
                }

                independently { removeAllSessions(includeLibraryModules, diagnosticInformation) }
            }
        } finally {
            // The session invalidation event is published in a `finally` to ensure that `KaFirSession`s are invalidated even when LL FIR
            // session invalidation partially fails. This is an essential part of exception resilience.
            //
            // We could also take `includeLibraryModules` into account here, but this will complicate the handling of the global session
            // invalidation event, and it isn't currently necessary for `KaFirSession` invalidation to be more granular.
            project.analysisMessageBus.syncPublisher(LLFirSessionInvalidationTopics.SESSION_INVALIDATION).afterGlobalInvalidation()
        }
    }

    private fun invalidateContextualDanglingFileSessions(contextModule: KaModule) = performInvalidation {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        sessionInvalidationEventPublisher.collectSessionsAndPublishInvalidationEvent {
            removeContextualDanglingFileSessions(contextModule)
        }
    }

    fun invalidateUnstableDanglingFileSessions() = performInvalidation {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        // We don't need to publish any session invalidation events for unstable dangling file modules.
        removeUnstableDanglingFileSessions()
    }

    /**
     * Ensures that the invalidated [block] does not run concurrently and cannot be canceled.
     *
     * Invalidation caused by a listener only happens in a write action. However, `KaFirCacheCleaner` can perform immediate invalidation
     * from outside one if there are no ongoing analyses. There might happen a situation when both an invalidation listener and the
     * stop-world cache cleaner request invalidation at once.
     *
     * The synchronization protects concurrent-unsafe cleanup in [SessionStorage].
     */
    private inline fun performInvalidation(crossinline block: () -> Unit) {
        synchronized(this) {
            // Any exception from session invalidation is an error that should be logged (even control flow exceptions). Since an exception
            // interrupts session invalidation, session caches might be left in an inconsistent state. See KT-78994.
            //
            // This is temporary code to gather more diagnostic data.
            try {
                // Cancellation exceptions can bring session caches into an inconsistent state (see KT-78994). Conceptually, session
                // invalidation is a critical operation that must not be interrupted. Historically, this was not an issue because session
                // invalidation was performed only in write actions. However, with low-memory cache cleanup, session invalidation can be
                // executed in a read action.
                //
                // Note that this does not prevent all cancellation exceptions, for example, when awaiting on an external cancellable
                // resource. However, it covers all known cases of such exceptions occurring during session invalidation.
                ProgressManager.getInstance().executeNonCancelableSection {
                    block()
                }
            } catch (t: Throwable) {
                LOG.error("Exception from LL FIR session invalidation!", t)
                throw t
            }
        }
    }

    /**
     * Removes the session(s) associated with [module] after it has been invalidated. Must be called in a write action.
     *
     * @return `true` if any sessions were removed.
     */
    private fun removeSession(module: KaModule): Boolean {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        val didSourceSessionExist = removeSourceSessionInWriteAction(module)

        val didOtherSessionExist = when (module) {
            is KaLibraryModule -> removeSessionFrom(module, storage.binaryCache)
            is KaLibraryFallbackDependenciesModule -> removeSessionFrom(module, storage.libraryFallbackDependenciesCache)
            is KaDanglingFileModule -> {
                val didStableSessionExist = removeSessionFrom(module, storage.danglingFileSessionCache)
                val didUnstableSessionExist = removeSessionFrom(module, storage.unstableDanglingFileSessionCache)
                didStableSessionExist || didUnstableSessionExist
            }
            else -> false
        }

        return didSourceSessionExist || didOtherSessionExist
    }

    /**
     * Removes the source session associated with the given [module] after it has been invalidated. Must be called in a write action.
     *
     * @return `true` if the source session was removed successfully.
     */
    private fun removeSourceSession(module: KaModule): Boolean {
        ApplicationManager.getApplication().assertWriteAccessAllowed()
        return removeSourceSessionInWriteAction(module)
    }

    private fun removeSourceSessionInWriteAction(module: KaModule): Boolean = removeSessionFrom(module, storage.sourceCache)

    private fun removeSessionFrom(module: KaModule, storage: SessionStorage): Boolean = storage.remove(module) != null

    /**
     * Removes all sessions after global invalidation. If [includeLibraryModules] is `false`, sessions of library modules will not be
     * removed.
     *
     * [removeAllSessions] must be called in a write action, or in the case if the caller can guarantee no other threads can perform
     * invalidation or code analysis until the cleanup is complete.
     */
    private fun removeAllSessions(includeLibraryModules: Boolean, diagnosticInformation: String? = null) {
        withIndependentRemoval {
            if (includeLibraryModules) {
                independently { removeAllSessionsFrom(storage.sourceCache, diagnosticInformation) }
                independently { removeAllSessionsFrom(storage.binaryCache, diagnosticInformation) }
                independently { removeAllLibraryFallbackDependenciesSessions() }
            } else {
                // `binaryCache` and `libraryFallbackDependenciesCache` can only contain library modules, so we only need to remove sessions
                // from `sourceCache`.
                independently {
                    removeAllMatchingSessionsFrom(storage.sourceCache) { it !is KaLibraryModule && it !is KaLibrarySourceModule }
                }
            }

            independently { removeAllDanglingFileSessions() }
        }
    }

    private fun removeUnstableDanglingFileSessions() {
        removeAllSessionsFrom(storage.unstableDanglingFileSessionCache)
    }

    private fun removeContextualDanglingFileSessions(contextModule: KaModule) {
        removeUnstableDanglingFileSessions()

        if (contextModule is KaDanglingFileModule) {
            removeAllMatchingSessionsFrom(storage.danglingFileSessionCache) {
                it is KaDanglingFileModule && hasContextModule(
                    it,
                    contextModule
                )
            }
        } else {
            // Only code fragments can have a dangling file context
            removeAllMatchingSessionsFrom(storage.danglingFileSessionCache) { it is KaDanglingFileModule && it.isCodeFragment }
        }
    }

    private tailrec fun hasContextModule(module: KaDanglingFileModule, contextModule: KaModule): Boolean {
        return when (val candidate = module.contextModule) {
            contextModule -> true
            is KaDanglingFileModule -> hasContextModule(candidate, contextModule)
            else -> false
        }
    }

    private fun removeAllDanglingFileSessions() {
        withIndependentRemoval {
            independently { removeAllSessionsFrom(storage.danglingFileSessionCache) }
            independently { removeAllSessionsFrom(storage.unstableDanglingFileSessionCache) }
        }
    }

    // Removing script sessions is only needed temporarily until KTIJ-25620 has been implemented.
    private fun removeAllScriptSessions() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        removeAllScriptSessionsFrom(storage.sourceCache)
        removeAllScriptSessionsFrom(storage.binaryCache)
    }

    private fun removeAllScriptSessionsFrom(storage: SessionStorage) {
        removeAllMatchingSessionsFrom(storage) { it is KaScriptModule || it is KaScriptDependencyModule }
    }

    private fun removeAllLibraryFallbackDependenciesSessions() {
        removeAllSessionsFrom(storage.libraryFallbackDependenciesCache)
    }

    /**
     * Removes all resolvable sessions for [KaLibraryModule]s and [KaLibrarySourceModule]s from the session cache. The function does not
     * affect *binary* library sessions.
     */
    private fun removeAllResolvableLibrarySessions() {
        removeAllMatchingSessionsFrom(storage.sourceCache) { it is KaLibraryModule || it is KaLibrarySourceModule }
    }

    private fun removeAllSessionsFrom(storage: SessionStorage, diagnosticInformation: String? = null) {
        storage.clear(diagnosticInformation)
    }

    private inline fun removeAllMatchingSessionsFrom(storage: SessionStorage, shouldBeRemoved: (KaModule) -> Boolean) {
        // Because this function is executed in a single thread, we do not need concurrency guarantees to remove all matching sessions, so a
        // "collect and remove" approach also works.
        withIndependentRemoval {
            storage.keys.forEach { module ->
                if (shouldBeRemoved(module)) {
                    independently { storage.remove(module) }
                }
            }
        }
    }

    /**
     * Allows running multiple different removal operations with [independently] while avoiding interference caused by exceptions. This
     * ensures that all removal operations can run during an invalidation operation, supporting cache consistency.
     *
     * Exceptions disrupting session invalidation have so far only been encountered outside write actions, so at this time, usage of this
     * function is limited to cases where invalidation is performed outside a write action.
     */
    private inline fun withIndependentRemoval(block: MutableList<Result<Unit>>.() -> Unit) {
        val results = buildList {
            block()
        }

        results.forEach { result -> result.onFailure { throw it } }
    }

    /**
     * Runs [block], deferring any exception until it is handled by [withIndependentRemoval] after all operations have been run.
     */
    private inline fun MutableList<Result<Unit>>.independently(block: () -> Unit) {
        val result = runCatching { block() }
        add(result)
    }

    companion object {
        private val LOG = logger<LLFirSessionCacheStorageInvalidator>()
    }
}
