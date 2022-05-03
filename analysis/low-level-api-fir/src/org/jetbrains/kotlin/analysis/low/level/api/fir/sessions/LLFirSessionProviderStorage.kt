/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySessionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.firKtModuleBasedModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.addValueFor
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.executeWithoutPCE
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.createLibrariesModificationTracker
import org.jetbrains.kotlin.analysis.providers.createModuleWithoutDependenciesOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.getValue
import org.jetbrains.kotlin.analysis.utils.caches.softCachedValue
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import java.util.concurrent.ConcurrentHashMap

class LLFirSessionProviderStorage(val project: Project) {
    private val sessionsCache = ConcurrentHashMap<KtModule, FromModuleViewSessionCache>()

    private val librariesFactoryByUseSiteModule by softCachedValue(project, project.createLibrariesModificationTracker()) {
        ConcurrentHashMap<KtModule, LLFirLibrarySessionFactory>()
    }

    fun getSessionProvider(
        useSiteKtModule: KtModule,
        configureSession: (LLFirSession.() -> Unit)? = null
    ): LLFirSessionProvider {
        val globalComponents = LLFirGlobalResolveComponents(useSiteKtModule, project)

        val librariesSessionFactory = librariesFactoryByUseSiteModule.getOrPut(useSiteKtModule) {
            val languageVersionSettings = when (useSiteKtModule) {
                is KtSourceModule -> useSiteKtModule.languageVersionSettings
                is KtLibraryModule, is KtLibrarySourceModule -> LanguageVersionSettingsImpl.DEFAULT
                else -> unexpectedElementError("module", useSiteKtModule)
            }
            LLFirLibrarySessionFactory(project, useSiteKtModule, languageVersionSettings)
        }

        val cache = sessionsCache.getOrPut(useSiteKtModule) { FromModuleViewSessionCache() }
        val (sessions, session) = cache.withMappings(project) { mappings ->
            val sessions = mutableMapOf<KtModule, LLFirResolvableModuleSession>().apply { putAll(mappings) }
            val session = executeWithoutPCE {
                when (useSiteKtModule) {
                    is KtSourceModule -> {
                        LLFirSessionFactory.createSourcesSession(
                            project,
                            useSiteKtModule,
                            globalComponents,
                            cache.sessionInvalidator,
                            sessions,
                            librariesSessionFactory = librariesSessionFactory,
                            configureSession = configureSession,
                        )
                    }
                    is KtLibraryModule, is KtLibrarySourceModule -> {
                        LLFirSessionFactory.createLibraryOrLibrarySourceResolvableSession(
                            project,
                            useSiteKtModule,
                            librariesSessionFactory.builtinsAndCloneableSession,
                            globalComponents,
                            cache.sessionInvalidator,
                            librariesSessionFactory.builtInTypes,
                            sessions,
                            configureSession = configureSession,
                        )
                    }
                    else -> error("Unexpected ${useSiteKtModule::class.simpleName}")
                }

            }
            sessions to session
        }

        return LLFirSessionProvider(project, session, sessions)
    }
}

private class FromModuleViewSessionCache {
    @Volatile
    private var mappings: PersistentMap<KtModule, FirSessionWithModificationTracker> = persistentMapOf()

    val sessionInvalidator: LLFirSessionInvalidator = LLFirSessionInvalidator { session ->
        mappings[session.firKtModuleBasedModuleData.ktModule]?.invalidate()
    }


    inline fun <R> withMappings(
        project: Project,
        action: (Map<KtModule, LLFirResolvableModuleSession>) -> Pair<Map<KtModule, LLFirResolvableModuleSession>, R>
    ): Pair<Map<KtModule, LLFirResolvableModuleSession>, R> {
        val (newMappings, result) = action(getSessions().mapValues { it.value })
        mappings = newMappings.mapValues { FirSessionWithModificationTracker(project, it.value) }.toPersistentMap()
        return newMappings to result
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getSessions(): Map<KtModule, LLFirResolvableModuleSession> = buildMap {
        val sessions = mappings.values
        val wasSessionInvalidated = sessions.associateWithTo(hashMapOf()) { false }

        val reversedDependencies = sessions.reversedDependencies { session ->
            session.firSession.module.directRegularDependencies.mapNotNull { mappings[it] }
        }

        fun markAsInvalidWithDfs(session: FirSessionWithModificationTracker) {
            if (wasSessionInvalidated.getValue(session)) {
                // we already was in that branch
                return
            }
            wasSessionInvalidated[session] = true
            reversedDependencies[session]?.forEach { dependsOn ->
                markAsInvalidWithDfs(dependsOn)
            }
        }

        for (session in sessions) {
            if (!session.isValid) {
                markAsInvalidWithDfs(session)
            }
        }
        return wasSessionInvalidated.entries
            .mapNotNull { (session, wasInvalidated) -> session.takeUnless { wasInvalidated } }
            .associate { session -> session.firSession.firKtModuleBasedModuleData.ktModule to session.firSession }
    }

    private fun <T> Collection<T>.reversedDependencies(getDependencies: (T) -> List<T>): Map<T, List<T>> {
        val result = hashMapOf<T, MutableList<T>>()
        forEach { from ->
            getDependencies(from).forEach { to ->
                result.addValueFor(to, from)
            }
        }
        return result
    }
}

private class FirSessionWithModificationTracker(
    project: Project,
    val firSession: LLFirResolvableModuleSession,
) {
    private val modificationTracker =
        when (val ktModule = firSession.firKtModuleBasedModuleData.ktModule) {
            is KtSourceModule -> ktModule.createModuleWithoutDependenciesOutOfBlockModificationTracker(project)
            else -> ModificationTracker.NEVER_CHANGED
        }


    private val timeStamp = modificationTracker.modificationCount

    @Volatile
    private var isInvalidated = false

    fun invalidate() {
        isInvalidated = true
    }

    val isValid: Boolean get() = !isInvalidated && modificationTracker.modificationCount == timeStamp
}