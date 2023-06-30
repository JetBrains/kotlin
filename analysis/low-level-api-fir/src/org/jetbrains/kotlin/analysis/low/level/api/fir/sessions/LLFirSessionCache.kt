/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.containers.CollectionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirInternals
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.checkCanceled
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.fir.FirModuleDataImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.PrivateSessionConstructor
import org.jetbrains.kotlin.fir.session.registerModuleData
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import java.util.concurrent.ConcurrentMap

private typealias SessionStorage = ConcurrentMap<KtModule, LLFirSession>

@LLFirInternals
class LLFirSessionCache(private val project: Project) {
    companion object {
        fun getInstance(project: Project): LLFirSessionCache {
            return project.getService(LLFirSessionCache::class.java)
        }
    }

    private val sourceCache: SessionStorage = CollectionFactory.createConcurrentSoftValueMap()
    private val binaryCache: SessionStorage = CollectionFactory.createConcurrentSoftValueMap()

    /**
     * Returns the existing session if found, or creates a new session and caches it.
     * Analyzable session will be returned for a library module.
     */
    fun getSession(module: KtModule, preferBinary: Boolean = false): LLFirSession {
        if (module is KtBinaryModule && (preferBinary || module is KtSdkModule)) {
            return getCachedSession(module, binaryCache) {
                createPlatformAwareSessionFactory(module).createBinaryLibrarySession(module)
            }
        }

        return getCachedSession(module, sourceCache, ::createSession)
    }

    /**
     * Returns a session without caching it.
     * Note that session dependencies are still cached.
     */
    internal fun getSessionNoCaching(module: KtModule): LLFirSession {
        return createSession(module)
    }

    private fun <T : KtModule> getCachedSession(
        module: T,
        storage: SessionStorage,
        factory: (T) -> LLFirSession
    ): LLFirSession {
        checkCanceled()

        return storage.computeIfAbsent(module) { factory(module) }.also { session ->
            require(session.isValid) { "A session acquired via `getSession` should always be valid. Module: $module" }
        }
    }

    /**
     * Removes the session(s) associated with [module] after it has been invalidated. Must be called in a write action.
     *
     * @return `true` if any sessions were removed.
     */
    fun removeSession(module: KtModule): Boolean {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        val didSourceSessionExist = removeSessionFrom(module, sourceCache)
        val didBinarySessionExist = if (module is KtBinaryModule) removeSessionFrom(module, binaryCache) else false

        return didSourceSessionExist || didBinarySessionExist
    }

    private fun removeSessionFrom(module: KtModule, storage: SessionStorage): Boolean {
        val session = storage.remove(module) ?: return false
        session.isValid = false
        return true
    }

    /**
     * Removes all sessions after global invalidation. If [includeBinarySessions] is `false`, only source sessions will be removed.
     *
     * [removeAllSessions] must be called in a write action.
     */
    fun removeAllSessions(includeBinarySessions: Boolean) {
        ApplicationManager.getApplication().assertWriteAccessAllowed()

        removeAllSessionsFrom(sourceCache)
        if (includeBinarySessions) removeAllSessionsFrom(binaryCache)
    }

    private fun removeAllSessionsFrom(storage: SessionStorage) {
        // Because `removeAllSessionsFrom` is executed in a write action, the order of setting `isValid` and clearing `storage` is not
        // important.
        storage.values.forEach { it.isValid = false }
        storage.clear()
    }

    private fun createSession(module: KtModule): LLFirSession {
        val sessionFactory = createPlatformAwareSessionFactory(module)
        return when (module) {
            is KtSourceModule -> sessionFactory.createSourcesSession(module)
            is KtLibraryModule, is KtLibrarySourceModule -> sessionFactory.createLibrarySession(module)
            is KtSdkModule -> sessionFactory.createBinaryLibrarySession(module)
            is KtScriptModule -> sessionFactory.createScriptSession(module)
            is KtNotUnderContentRootModule -> sessionFactory.createNotUnderContentRootResolvableSession(module)
            else -> error("Unexpected module kind: ${module::class.simpleName}")
        }
    }

    private fun createPlatformAwareSessionFactory(module: KtModule): LLFirAbstractSessionFactory {
        val targetPlatform = module.platform
        return when {
            targetPlatform.all { it is JvmPlatform } -> LLFirJvmSessionFactory(project)
            targetPlatform.all { it is JsPlatform } -> LLFirJsSessionFactory(project)
            targetPlatform.all { it is NativePlatform } -> LLFirNativeSessionFactory(project)
            else -> LLFirCommonSessionFactory(project)
        }
    }
}

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
            analyzerServices = JvmPlatformAnalyzerServices
        )
        registerModuleData(moduleData)
        moduleData.bindSession(this)
    }
}
