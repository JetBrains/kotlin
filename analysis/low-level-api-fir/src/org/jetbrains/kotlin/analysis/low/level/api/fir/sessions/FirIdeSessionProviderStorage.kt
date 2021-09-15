/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import org.jetbrains.kotlin.analysis.providers.createLibrariesModificationTracker
import org.jetbrains.kotlin.analysis.providers.createModuleWithoutDependenciesOutOfBlockModificationTracker
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.FirPhaseRunner
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.addValueFor
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.executeWithoutPCE
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import java.util.concurrent.ConcurrentHashMap

class FirIdeSessionProviderStorage(val project: Project) {
    private val sessionsCache = ConcurrentHashMap<KtSourceModule, FromModuleViewSessionCache>()

    private val librariesCache by cachedValue(project, project.createLibrariesModificationTracker()) { LibrariesCache() }

    fun getSessionProvider(
        rootModule: KtSourceModule,
        configureSession: (FirIdeSession.() -> Unit)? = null
    ): FirIdeSessionProvider {
        val firPhaseRunner = FirPhaseRunner()

        val builtinTypes = BuiltinTypes()
        val builtinsAndCloneableSession = FirIdeSessionFactory.createBuiltinsAndCloneableSession(project, builtinTypes)
        val cache = sessionsCache.getOrPut(rootModule) { FromModuleViewSessionCache(rootModule) }
        val (sessions, session) = cache.withMappings(project) { mappings ->
            val sessions = mutableMapOf<KtSourceModule, FirIdeSourcesSession>().apply { putAll(mappings) }
            val session = executeWithoutPCE {
                FirIdeSessionFactory.createSourcesSession(
                    project,
                    rootModule,
                    builtinsAndCloneableSession,
                    firPhaseRunner,
                    cache.sessionInvalidator,
                    builtinTypes,
                    sessions,
                    isRootModule = true,
                    librariesCache = librariesCache,
                    configureSession = configureSession,
                )
            }
            sessions to session
        }

        return FirIdeSessionProvider(project, session, sessions)
    }
}

private class FromModuleViewSessionCache(
    val root: KtSourceModule,
) {
    @Volatile
    private var mappings: PersistentMap<KtSourceModule, FirSessionWithModificationTracker> = persistentMapOf()

    val sessionInvalidator: FirSessionInvalidator = FirSessionInvalidator { session ->
        mappings[session.moduleData.module]?.invalidate()
    }


    inline fun <R> withMappings(
        project: Project,
        action: (Map<KtSourceModule, FirIdeSourcesSession>) -> Pair<Map<KtSourceModule, FirIdeSourcesSession>, R>
    ): Pair<Map<KtSourceModule, FirIdeSourcesSession>, R> {
        val (newMappings, result) = action(getSessions().mapValues { it.value })
        mappings = newMappings.mapValues { FirSessionWithModificationTracker(project, it.value) }.toPersistentMap()
        return newMappings to result
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getSessions(): Map<KtSourceModule, FirIdeSourcesSession> = buildMap {
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
            .associate { session -> session.firSession.moduleData.module to session.firSession }
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
    val firSession: FirIdeSourcesSession,
) {
    private val modificationTracker =
        firSession.moduleData.module.createModuleWithoutDependenciesOutOfBlockModificationTracker(project)

    private val timeStamp = modificationTracker.modificationCount

    @Volatile
    private var isInvalidated = false

    fun invalidate() {
        isInvalidated = true
    }

    val isValid: Boolean get() = !isInvalidated && modificationTracker.modificationCount == timeStamp
}

internal val FirModuleData.module: KtSourceModule get() = moduleUnsafe()

internal inline fun <reified T : KtModule> FirModuleData.moduleUnsafe(): T = (this as KtModuleBasedModuleData).module as T
internal inline fun <reified T : KtModule> FirModuleData.moduleInfoSafe(): T? = (this as KtModuleBasedModuleData).module as? T
