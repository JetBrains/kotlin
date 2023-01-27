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
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirBuiltinsSessionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySessionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.addValueFor
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.providers.KotlinModificationTrackerFactory
import org.jetbrains.kotlin.analysis.providers.KtModuleStateTracker
import org.jetbrains.kotlin.analysis.utils.trackers.CompositeModificationTracker
import java.lang.ref.SoftReference

class LLFirSessionProviderStorage(val project: Project) {
    private val sourceAsUseSiteSessionCache = LLFirSessionsCache()
    private val libraryAsUseSiteSessionCache = LLFirSessionsCache()
    private val notUnderContentRootSessionCache = LLFirSessionsCache()

    private val librariesSessionFactory = LLFirLibrarySessionFactory.getInstance(project)
    private val builtInsSessionFactory = LLFirBuiltinsSessionFactory.getInstance(project)

    private val globalComponents = LLFirGlobalResolveComponents(project)

    fun getSessionProvider(
        useSiteKtModule: KtModule,
        configureSession: (LLFirSession.() -> Unit)? = null
    ): LLFirSessionProvider = when (useSiteKtModule) {
        is KtSourceModule -> {
            createSessionProviderForSourceSession(useSiteKtModule, configureSession)
        }

        is KtLibraryModule, is KtLibrarySourceModule -> {
            createSessionProviderForLibraryOrLibrarySource(useSiteKtModule, configureSession)
        }

        is KtNotUnderContentRootModule -> {
            createSessionProviderForNotUnderContentRootSession(useSiteKtModule, configureSession)
        }

        else -> error("Unexpected ${useSiteKtModule::class.simpleName}")
    }


    private fun createSessionProviderForSourceSession(
        useSiteKtModule: KtSourceModule,
        configureSession: (LLFirSession.() -> Unit)?
    ): LLFirSessionProvider {
        val (sessions, session) = sourceAsUseSiteSessionCache.withMappings(project) { mappings ->
            val sessions = mutableMapOf<KtModule, LLFirSession>().apply { putAll(mappings) }
            val session = LLFirSessionFactory.createSourcesSession(
                project,
                useSiteKtModule,
                globalComponents,
                sourceAsUseSiteSessionCache.sessionInvalidator,
                sessions,
                librariesSessionFactory,
                configureSession = configureSession,
            )
            sessions to session
        }
        return LLFirSessionProvider(project, session, KtModuleToSessionMappingByWeakValueMapImpl(sessions))
    }


    private fun createSessionProviderForLibraryOrLibrarySource(
        useSiteKtModule: KtModule,
        configureSession: (LLFirSession.() -> Unit)?
    ): LLFirSessionProvider {
        val (sessions, session) = libraryAsUseSiteSessionCache.withMappings(project) { mappings ->
            val sessions = mutableMapOf<KtModule, LLFirSession>().apply { putAll(mappings) }
            val session = LLFirSessionFactory.createLibraryOrLibrarySourceResolvableSession(
                project,
                useSiteKtModule,
                globalComponents,
                libraryAsUseSiteSessionCache.sessionInvalidator,
                builtInsSessionFactory.getBuiltinsSession(useSiteKtModule.platform),
                sessions,
                configureSession = configureSession,
            )
            sessions to session
        }
        return LLFirSessionProvider(project, session, KtModuleToSessionMappingByWeakValueMapImpl(sessions))
    }

    private fun createSessionProviderForNotUnderContentRootSession(
        useSiteKtModule: KtNotUnderContentRootModule,
        configureSession: (LLFirSession.() -> Unit)?
    ): LLFirSessionProvider {
        val (sessions, session) = notUnderContentRootSessionCache.withMappings(project) { mappings ->
            val sessions = mutableMapOf<KtModule, LLFirSession>().apply { putAll(mappings) }
            val session = LLFirSessionFactory.createNotUnderContentRootResolvableSession(
                project,
                useSiteKtModule,
                notUnderContentRootSessionCache.sessionInvalidator,
                sessions,
                configureSession = configureSession,
            )
            sessions to session
        }
        return LLFirSessionProvider(project, session, KtModuleToSessionMappingByWeakValueMapImpl(sessions))
    }
}

private class LLFirSessionsCache {
    @Volatile
    private var mappings: PersistentMap<KtModule, FirSessionWithModificationTracker> = persistentMapOf()

    val sessionInvalidator: LLFirSessionInvalidator = LLFirSessionInvalidator { session ->
        mappings[session.llFirModuleData.ktModule]?.invalidate()
    }


    inline fun <R> withMappings(
        project: Project,
        action: (Map<KtModule, LLFirSession>) -> Pair<Map<KtModule, LLFirSession>, R>
    ): Pair<Map<KtModule, LLFirSession>, R> {
        val (newMappings, result) = action(getSessions().mapValues { it.value })
        mappings = newMappings.mapValues { FirSessionWithModificationTracker(project, it.value) }.toPersistentMap()
        return newMappings to result
    }

    private fun getSessions(): Map<KtModule, LLFirSession> = buildMap {
        val sessions = mappings.values
        val wasSessionInvalidated = sessions.associateWithTo(hashMapOf()) { false }

        val reversedDependencies = sessions.reversedDependencies { session ->
            if (session.validityTracker.isValid) {
                session.ktModule.directRegularDependencies.mapNotNull { mappings[it] }
            } else emptyList()
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
            .mapNotNull { (sessionWithTracker, wasInvalidated) ->
                if (wasInvalidated) return@mapNotNull null
                val firSession = sessionWithTracker.firSessionSoftReference.get() ?: return@mapNotNull null
                sessionWithTracker.ktModule to firSession
            }.toMap()
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
    firSession: LLFirSession,
) {
    val firSessionSoftReference: SoftReference<LLFirSession> = SoftReference(firSession)
    val ktModule = firSession.llFirModuleData.ktModule

    val validityTracker: KtModuleStateTracker
    private val modificationTracker: ModificationTracker

    init {
        val trackerFactory = KotlinModificationTrackerFactory.getService(project)

        validityTracker = trackerFactory.createModuleStateTracker(ktModule)

        val outOfBlockTracker = when (ktModule) {
            is KtSourceModule -> trackerFactory.createModuleWithoutDependenciesOutOfBlockModificationTracker(ktModule)
            is KtNotUnderContentRootModule -> ModificationTracker { ktModule.file?.modificationStamp ?: 0 }
            else -> null
        }
        modificationTracker = CompositeModificationTracker.create(
            listOfNotNull(
                outOfBlockTracker,
                object : ModificationTracker {
                    override fun getModificationCount() = validityTracker.rootModificationCount
                }
            )
        )
    }


    private val timeStamp = modificationTracker.modificationCount

    @Volatile
    private var isInvalidated = false

    fun invalidate() {
        isInvalidated = true
    }

    val isValid: Boolean
        get() = validityTracker.isValid
                && !isInvalidated
                && modificationTracker.modificationCount == timeStamp
}