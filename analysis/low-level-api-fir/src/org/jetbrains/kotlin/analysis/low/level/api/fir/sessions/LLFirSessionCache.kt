/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptDependencyModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.isStable
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.caches.CleanableSoftValueCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirBuiltinsSessionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.fir.FirModuleDataImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.session.registerModuleData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatform

private typealias SessionStorage = CleanableSoftValueCache<KaModule, LLFirSession>

@LLFirInternals
class LLFirSessionCache(private val project: Project) : Disposable {
    companion object {
        fun getInstance(project: Project): LLFirSessionCache {
            return project.getService(LLFirSessionCache::class.java)
        }
    }

    // Removal from the session storage invokes the `LLFirSession`'s cleaner, which marks the session as invalid and disposes any
    // disposables registered with the `LLFirSession`'s disposable.
    private val sourceCache: SessionStorage = CleanableSoftValueCache(LLFirSession::createSessionCleaner)
    private val binaryCache: SessionStorage = CleanableSoftValueCache(LLFirSession::createSessionCleaner)
    private val danglingFileSessionCache: SessionStorage = CleanableSoftValueCache(LLFirSession::createSessionCleaner)
    private val unstableDanglingFileSessionCache: SessionStorage = CleanableSoftValueCache(LLFirSession::createSessionCleaner)

    /**
     * Returns the existing session if found, or creates a new session and caches it.
     * Analyzable session will be returned for a library module.
     *
     * Must be called from a read action.
     */
    fun getSession(module: KaModule, preferBinary: Boolean = false): LLFirSession {
        if (module is KaBuiltinsModule && preferBinary) {
            return LLFirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(module.targetPlatform)
        }

        if (module is KaLibraryModule && (preferBinary || module.isSdk)) {
            return getCachedSession(module, binaryCache) {
                createPlatformAwareSessionFactory(module).createBinaryLibrarySession(module)
            }
        }

        if (module is KaDanglingFileModule) {
            return getDanglingFileCachedSession(module)
        }

        return getCachedSession(module, sourceCache, factory = ::createSession)
    }

    /**
     * Returns a session without caching it.
     * Note that session dependencies are still cached.
     */
    internal fun getSessionNoCaching(module: KaModule): LLFirSession {
        return createSession(module)
    }

    private fun getDanglingFileCachedSession(module: KaDanglingFileModule): LLFirSession {
        if (module.isStable) {
            return getCachedSession(module, danglingFileSessionCache, ::createSession)
        }

        checkCanceled()

        val session = unstableDanglingFileSessionCache.compute(module) { _, existingSession ->
            if (existingSession is LLFirDanglingFileSession && existingSession.modificationStamp == module.file.modificationStamp) {
                existingSession
            } else {
                createSession(module)
            }
        }

        requireNotNull(session)
        checkSessionValidity(session)

        return session
    }

    private fun <T : KaModule> getCachedSession(module: T, storage: SessionStorage, factory: (T) -> LLFirSession): LLFirSession {
        checkCanceled()

        val session = if (module.supportsIsolatedSessionCreation) {
            storage.computeIfAbsent(module) { factory(module) }
        } else {
            // Non-isolated session creation may need to access other sessions, so we should create the session outside `computeIfAbsent` to
            // avoid recursive update exceptions.
            storage[module] ?: run {
                val newSession = factory(module)
                storage.computeIfAbsent(module) { newSession }
            }
        }

        checkSessionValidity(session)
        return session
    }

    private fun checkSessionValidity(session: LLFirSession) {
        require(session.isValid) { "A session acquired via `getSession` should always be valid. Module: ${session.ktModule}" }
    }

    /**
     * Removes the session(s) associated with [module] after it has been invalidated. Must be called in a write action.
     *
     * @return `true` if any sessions were removed.
     */
    fun removeSession(module: KaModule): Boolean {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        val didSourceSessionExist = removeSessionFrom(module, sourceCache)
        val didBinarySessionExist = module is KaLibraryModule && removeSessionFrom(module, binaryCache)
        val didDanglingFileSessionExist = module is KaDanglingFileModule && removeSessionFrom(module, danglingFileSessionCache)
        val didUnstableDanglingFileSessionExist = module is KaDanglingFileModule && removeSessionFrom(module, unstableDanglingFileSessionCache)

        return didSourceSessionExist || didBinarySessionExist || didDanglingFileSessionExist || didUnstableDanglingFileSessionExist
    }

    private fun removeSessionFrom(module: KaModule, storage: SessionStorage): Boolean = storage.remove(module) != null

    /**
     * Removes all sessions after global invalidation. If [includeLibraryModules] is `false`, sessions of library modules will not be
     * removed.
     *
     * [removeAllSessions] must be called in a write action.
     */
    fun removeAllSessions(includeLibraryModules: Boolean) {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        if (includeLibraryModules) {
            removeAllSessionsFrom(sourceCache)
            removeAllSessionsFrom(binaryCache)
        } else {
            // `binaryCache` can only contain library modules, so we only need to remove sessions from `sourceCache`.
            removeAllMatchingSessionsFrom(sourceCache) { it !is KaLibraryModule && it !is KaLibrarySourceModule }
        }

        removeAllDanglingFileSessions()
    }

    fun removeUnstableDanglingFileSessions() {
        removeAllSessionsFrom(unstableDanglingFileSessionCache)
    }

    fun removeContextualDanglingFileSessions(contextModule: KaModule) {
        removeUnstableDanglingFileSessions()

        if (contextModule is KaDanglingFileModule) {
            removeAllMatchingSessionsFrom(danglingFileSessionCache) { it is KaDanglingFileModule && hasContextModule(it, contextModule) }
        } else {
            // Only code fragments can have a dangling file context
            removeAllMatchingSessionsFrom(danglingFileSessionCache) { it is KaDanglingFileModule && it.isCodeFragment }
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
        removeAllSessionsFrom(danglingFileSessionCache)
        removeAllSessionsFrom(unstableDanglingFileSessionCache)
    }

    // Removing script sessions is only needed temporarily until KTIJ-25620 has been implemented.
    fun removeAllScriptSessions() {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        removeAllScriptSessionsFrom(sourceCache)
        removeAllScriptSessionsFrom(binaryCache)
    }

    private fun removeAllScriptSessionsFrom(storage: SessionStorage) {
        removeAllMatchingSessionsFrom(storage) { it is KaScriptModule || it is KaScriptDependencyModule }
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

    /**
     * Whether the session for this [KaModule] can be created without getting other sessions from the cache. Should be kept in sync with
     * [createSession].
     */
    private val KaModule.supportsIsolatedSessionCreation: Boolean
        get() = this !is KaDanglingFileModule

    private fun createSession(module: KaModule): LLFirSession {
        val sessionFactory = createPlatformAwareSessionFactory(module)
        return when (module) {
            is KaSourceModule -> sessionFactory.createSourcesSession(module)
            is KaLibraryModule -> {
                if (module.isSdk) {
                    sessionFactory.createBinaryLibrarySession(module)
                } else {
                    sessionFactory.createLibrarySession(module)
                }
            }
            is KaLibrarySourceModule -> sessionFactory.createLibrarySession(module)
            is KaScriptModule -> sessionFactory.createScriptSession(module)
            is KaDanglingFileModule -> {
                //  Dangling file context must have an analyzable session, so we can properly compile code against it.
                val contextSession = getSession(module.contextModule, preferBinary = false)
                sessionFactory.createDanglingFileSession(module, contextSession)
            }
            is KaNotUnderContentRootModule -> sessionFactory.createNotUnderContentRootResolvableSession(module)
            else -> error("Unexpected module kind: ${module::class.simpleName}")
        }
    }

    private fun createPlatformAwareSessionFactory(module: KaModule): LLFirAbstractSessionFactory {
        val targetPlatform = module.targetPlatform
        return when {
            targetPlatform.all { it is JvmPlatform } -> LLFirJvmSessionFactory(project)
            targetPlatform.all { it is JsPlatform } -> LLFirJsSessionFactory(project)
            targetPlatform.all { it is NativePlatform } -> LLFirNativeSessionFactory(project)
            else -> LLFirCommonSessionFactory(project)
        }
    }

    override fun dispose() {
    }
}

private fun LLFirSession.createSessionCleaner(): LLFirSessionCleaner = LLFirSessionCleaner(requestedDisposableOrNull)

internal fun LLFirSessionConfigurator.Companion.configure(session: LLFirSession) {
    val project = session.project
    for (extension in extensionPointName.getExtensionList(project)) {
        extension.configure(session)
    }
}

@Deprecated(
    "This is a dirty hack used only for one usage (building fir for psi from stubs) and it should be removed after fix of that usage",
    level = DeprecationLevel.ERROR
)
@OptIn(PrivateSessionConstructor::class)
fun createEmptySession(): FirSession {
    return object : FirSession(null, Kind.Source) {}.apply {
        val moduleData = FirModuleDataImpl(
            Name.identifier("<stub module>"),
            dependencies = emptyList(),
            dependsOnDependencies = emptyList(),
            friendDependencies = emptyList(),
            platform = JvmPlatforms.unspecifiedJvmPlatform,
        )
        registerModuleData(moduleData)
        moduleData.bindSession(this)
    }
}
