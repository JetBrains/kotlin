/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals

@LLFirInternals
class LLFirSessionCacheStorageInvalidator(
    private val storage: LLFirSessionCacheStorage,
) {

    /**
     * Removes the session(s) associated with [module] after it has been invalidated. Must be called in a write action.
     *
     * @return `true` if any sessions were removed.
     */
    fun removeSession(module: KaModule): Boolean {
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
    fun removeSourceSession(module: KaModule): Boolean {
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
    fun removeAllSessions(includeLibraryModules: Boolean) {
        if (includeLibraryModules) {
            removeAllSessionsFrom(storage.sourceCache)
            removeAllSessionsFrom(storage.binaryCache)
            removeAllLibraryFallbackDependenciesSessions()
        } else {
            // `binaryCache` and `libraryFallbackDependenciesCache` can only contain library modules, so we only need to remove sessions
            // from `sourceCache`.
            removeAllMatchingSessionsFrom(storage.sourceCache) { it !is KaLibraryModule && it !is KaLibrarySourceModule }
        }

        removeAllDanglingFileSessions()
    }

    fun removeUnstableDanglingFileSessions() {
        removeAllSessionsFrom(storage.unstableDanglingFileSessionCache)
    }

    fun removeContextualDanglingFileSessions(contextModule: KaModule) {
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

    fun removeAllDanglingFileSessions() {
        removeAllSessionsFrom(storage.danglingFileSessionCache)
        removeAllSessionsFrom(storage.unstableDanglingFileSessionCache)
    }

    // Removing script sessions is only needed temporarily until KTIJ-25620 has been implemented.
    fun removeAllScriptSessions() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        removeAllScriptSessionsFrom(storage.sourceCache)
        removeAllScriptSessionsFrom(storage.binaryCache)
    }

    private fun removeAllScriptSessionsFrom(storage: SessionStorage) {
        removeAllMatchingSessionsFrom(storage) { it is KaScriptModule || it is KaScriptDependencyModule }
    }

    fun removeAllLibraryFallbackDependenciesSessions() {
        removeAllSessionsFrom(storage.libraryFallbackDependenciesCache)
    }

    /**
     * Removes all resolvable sessions for [KaLibraryModule]s and [KaLibrarySourceModule]s from the session cache. The function does not
     * affect *binary* library sessions.
     */
    fun removeAllResolvableLibrarySessions() {
        removeAllMatchingSessionsFrom(storage.sourceCache) { it is KaLibraryModule || it is KaLibrarySourceModule }
    }

    private fun removeAllSessionsFrom(storage: SessionStorage) {
        storage.clear()
    }

    private inline fun removeAllMatchingSessionsFrom(storage: SessionStorage, shouldBeRemoved: (KaModule) -> Boolean) {
        // Because this function is executed in a write action, we do not need concurrency guarantees to remove all matching sessions, so a
        // "collect and remove" approach also works.
        storage.keys.forEach { module ->
            if (shouldBeRemoved(module)) {
                storage.remove(module)
            }
        }
    }

}