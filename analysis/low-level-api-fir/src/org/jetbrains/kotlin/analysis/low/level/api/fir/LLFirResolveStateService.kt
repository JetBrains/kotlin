/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionProviderStorage
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirSourceResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirLibraryOrLibrarySourceResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.strongCachedValue
import java.util.concurrent.ConcurrentHashMap

internal class LLFirResolveSessionService(project: Project) {
    private val sessionProviderStorage = LLFirSessionProviderStorage(project)

    private val stateCache by strongCachedValue(
        project.createProjectWideOutOfBlockModificationTracker(),
        ProjectRootModificationTracker.getInstance(project),
    ) {
        ConcurrentHashMap<KtModule, LLFirResolvableResolveSession>()
    }

    fun getFirResolveSession(module: KtModule): LLFirResolvableResolveSession =
        stateCache.computeIfAbsent(module) { createFirResolveSessionFor(module, sessionProviderStorage) }

    companion object {
        fun getInstance(project: Project): LLFirResolveSessionService =
            ServiceManager.getService(project, LLFirResolveSessionService::class.java)

        internal fun createFirResolveSessionFor(
            useSiteKtModule: KtModule,
            sessionProviderStorage: LLFirSessionProviderStorage,
            configureSession: (LLFirSession.() -> Unit)? = null,
        ): LLFirResolvableResolveSession {
            val sessionProvider = sessionProviderStorage.getSessionProvider(useSiteKtModule, configureSession)
            val useSiteSession = sessionProvider.rootModuleSession
            return when (useSiteKtModule) {
                is KtSourceModule -> {
                    LLFirSourceResolveSession(
                        useSiteSession.moduleComponents.globalResolveComponents,
                        sessionProviderStorage.project,
                        useSiteKtModule,
                        sessionProvider,
                    )
                }
                is KtLibraryModule, is KtLibrarySourceModule -> {
                    LLFirLibraryOrLibrarySourceResolvableResolveSession(
                        useSiteSession.moduleComponents.globalResolveComponents,
                        sessionProviderStorage.project,
                        useSiteKtModule,
                        sessionProvider,
                    )
                }
                else -> {
                    error("Unexpected $useSiteKtModule")
                }
            }

        }
    }
}

@TestOnly
fun createFirResolveSessionForNoCaching(
    useSiteKtModule: KtModule,
    project: Project,
    configureSession: (LLFirSession.() -> Unit)? = null,
): LLFirResolveSession =
    LLFirResolveSessionService.createFirResolveSessionFor(
        useSiteKtModule = useSiteKtModule,
        sessionProviderStorage = LLFirSessionProviderStorage(project),
        configureSession = configureSession
    )


