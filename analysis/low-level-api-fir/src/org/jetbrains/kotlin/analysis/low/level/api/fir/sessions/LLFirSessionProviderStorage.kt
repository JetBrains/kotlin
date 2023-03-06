/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.sessions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirBuiltinsSessionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.LLFirLibrarySessionFactory
import org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure.llFirModuleData
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.addValueFor
import org.jetbrains.kotlin.analysis.project.structure.*
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

        is KtScriptModule -> {
            createSessionProviderForScriptSession(useSiteKtModule, configureSession)
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
        val (sessions, session) = sourceAsUseSiteSessionCache.withMappings { mappings ->
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
        val (sessions, session) = libraryAsUseSiteSessionCache.withMappings { mappings ->
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

    private fun createSessionProviderForScriptSession(
        useSiteKtModule: KtScriptModule,
        configureSession: (LLFirSession.() -> Unit)?
    ): LLFirSessionProvider {
        val (sessions, session) = sourceAsUseSiteSessionCache.withMappings { mappings ->
            val sessions = mutableMapOf<KtModule, LLFirSession>().apply { putAll(mappings) }
            val session = LLFirSessionFactory.createScriptSession(
                project,
                useSiteKtModule,
                sourceAsUseSiteSessionCache.sessionInvalidator,
                sessions,
                librariesSessionFactory,
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
        val (sessions, session) = notUnderContentRootSessionCache.withMappings { mappings ->
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
    private var mappings: Map<KtModule, SoftReference<LLFirSession>> = emptyMap()

    val sessionInvalidator: LLFirSessionInvalidator = LLFirSessionInvalidator { session ->
        mappings[session.llFirModuleData.ktModule]?.get()?.invalidate()
    }

    inline fun <R> withMappings(
        action: (Map<KtModule, LLFirSession>) -> Pair<Map<KtModule, LLFirSession>, R>
    ): Pair<Map<KtModule, LLFirSession>, R> {
        val (newMappings, result) = action(getSessions().mapValues { it.value })
        mappings = newMappings.mapValues { SoftReference(it.value) }
        return newMappings to result
    }

    private fun getSessions(): Map<KtModule, LLFirSession> = buildMap {
        // Initially, all sessions are considered to be valid ('true').
        val sessions = LinkedHashMap<LLFirSession, Boolean>().apply {
            for (sessionRef in mappings.values) {
                val session = sessionRef.get() ?: continue
                put(session, true)
            }
        }

        val reversedDependencies = buildMap {
            for (session in sessions.keys) {
                if (session.isValid) {
                    val module = session.ktModule
                    for (dependency in module.directRegularDependencies) {
                        addValueFor(dependency, module)
                    }
                }
            }
        }

        fun invalidateRecursively(session: LLFirSession) {
            // Invalidate all dependent sessions only if we didn't that before
            if (sessions.put(session, false) == true) {
                for (dependentModule in reversedDependencies[session.ktModule].orEmpty()) {
                    val dependentSession = mappings[dependentModule]?.get() ?: continue
                    invalidateRecursively(dependentSession)
                }
            }
        }

        for (session in sessions.keys) {
            if (session.isValid) continue
            invalidateRecursively(session)
        }

        return buildMap {
            for ((session, isValid) in sessions) {
                if (!isValid) continue
                put(session.ktModule, session)
            }
        }
    }
}