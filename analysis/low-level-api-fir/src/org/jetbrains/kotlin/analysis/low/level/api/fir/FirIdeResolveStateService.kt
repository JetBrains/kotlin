/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionProviderStorage
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSourcesSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirSourceModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirLibraryOrLibrarySourceResolvableModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableModuleResolveState
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.strongCachedValue
import java.util.concurrent.ConcurrentHashMap

internal class LLFirResolveStateService(project: Project) {
    private val sessionProviderStorage = LLFirSessionProviderStorage(project)

    private val stateCache by strongCachedValue(
        project.createProjectWideOutOfBlockModificationTracker(),
        ProjectRootModificationTracker.getInstance(project),
    ) {
        ConcurrentHashMap<KtModule, LLFirResolvableModuleResolveState>()
    }

    fun getResolveState(module: KtModule): LLFirResolvableModuleResolveState =
        stateCache.computeIfAbsent(module) { createResolveStateFor(module, sessionProviderStorage) }

    companion object {
        fun getInstance(project: Project): LLFirResolveStateService =
            ServiceManager.getService(project, LLFirResolveStateService::class.java)

        internal fun createResolveStateFor(
            module: KtModule,
            sessionProviderStorage: LLFirSessionProviderStorage,
            configureSession: (LLFirSession.() -> Unit)? = null,
        ): LLFirResolvableModuleResolveState {
            val sessionProvider = sessionProviderStorage.getSessionProvider(module, configureSession)
            return when (module) {
                is KtSourceModule -> {
                    val firFileBuilder = (sessionProvider.rootModuleSession as LLFirSourcesSession).firFileBuilder
                    LLFirSourceModuleResolveState(
                        sessionProviderStorage.project,
                        module,
                        sessionProvider,
                        firFileBuilder,
                        FirLazyDeclarationResolver(firFileBuilder),
                    )
                }
                is KtLibraryModule, is KtLibrarySourceModule -> {
                    val firFileBuilder = (sessionProvider.rootModuleSession as LLFirLibraryOrLibrarySourceResolvableModuleSession).firFileBuilder
                    LLFirLibraryOrLibrarySourceResolvableModuleResolveState(
                        sessionProviderStorage.project,
                        module,
                        sessionProvider,
                        firFileBuilder,
                        FirLazyDeclarationResolver(firFileBuilder),
                    )
                }
                else -> {
                    error("Unexpected $module")
                }
            }

        }
    }
}

@TestOnly
fun createResolveStateForNoCaching(
    module: KtModule,
    project: Project,
    configureSession: (LLFirSession.() -> Unit)? = null,
): LLFirModuleResolveState =
    LLFirResolveStateService.createResolveStateFor(
        module = module,
        sessionProviderStorage = LLFirSessionProviderStorage(project),
        configureSession = configureSession
    )


