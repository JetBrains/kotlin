/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.FirModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirIdeSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirIdeSessionProviderStorage
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.FirIdeSourcesSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.FirSourceModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirLibraryOrLibrarySourceResolvableModuleResolveState
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableModuleResolveState
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibrarySourceModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.providers.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.analysis.utils.caches.strongCachedValue
import java.util.concurrent.ConcurrentHashMap

internal class FirIdeResolveStateService(project: Project) {
    private val sessionProviderStorage = FirIdeSessionProviderStorage(project)

    private val stateCache by strongCachedValue(
        project.createProjectWideOutOfBlockModificationTracker(),
        ProjectRootModificationTracker.getInstance(project),
    ) {
        ConcurrentHashMap<KtModule, LLFirResolvableModuleResolveState>()
    }

    fun getResolveState(module: KtModule): LLFirResolvableModuleResolveState =
        stateCache.computeIfAbsent(module) { createResolveStateFor(module, sessionProviderStorage) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService =
            ServiceManager.getService(project, FirIdeResolveStateService::class.java)

        internal fun createResolveStateFor(
            module: KtModule,
            sessionProviderStorage: FirIdeSessionProviderStorage,
            configureSession: (FirIdeSession.() -> Unit)? = null,
        ): LLFirResolvableModuleResolveState {
            val sessionProvider = sessionProviderStorage.getSessionProvider(module, configureSession)
            return when (module) {
                is KtSourceModule -> {
                    val firFileBuilder = (sessionProvider.rootModuleSession as FirIdeSourcesSession).firFileBuilder
                    FirSourceModuleResolveState(
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
    configureSession: (FirIdeSession.() -> Unit)? = null,
): FirModuleResolveState =
    FirIdeResolveStateService.createResolveStateFor(
        module = module,
        sessionProviderStorage = FirIdeSessionProviderStorage(project),
        configureSession = configureSession
    )


