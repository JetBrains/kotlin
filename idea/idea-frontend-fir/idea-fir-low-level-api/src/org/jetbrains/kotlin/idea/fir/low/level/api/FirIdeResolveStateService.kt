/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ModuleSourceInfoBase
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.createProjectWideOutOfBlockModificationTracker
import org.jetbrains.kotlin.idea.fir.low.level.api.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSession
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.FirIdeSessionProviderStorage
import org.jetbrains.kotlin.idea.fir.low.level.api.util.*

import java.util.concurrent.ConcurrentHashMap

internal class FirIdeResolveStateService(project: Project) {
    private val sessionProviderStorage = FirIdeSessionProviderStorage(project)

    private val stateCache by cachedValue(
        project,
        project.createProjectWideOutOfBlockModificationTracker(),
        ProjectRootModificationTracker.getInstance(project),
    ) {
        ConcurrentHashMap<ModuleInfo, FirModuleResolveStateImpl>()
    }

    fun getResolveState(moduleInfo: ModuleInfo): FirModuleResolveStateImpl =
        stateCache.computeIfAbsent(moduleInfo) { createResolveStateFor(moduleInfo, sessionProviderStorage) }

    companion object {
        fun getInstance(project: Project): FirIdeResolveStateService =
            ServiceManager.getService(project, FirIdeResolveStateService::class.java)

        internal fun createResolveStateFor(
            moduleInfo: ModuleInfo,
            sessionProviderStorage: FirIdeSessionProviderStorage,
            configureSession: (FirIdeSession.() -> Unit)? = null,
        ): FirModuleResolveStateImpl {
            if (moduleInfo !is ModuleSourceInfoBase) {
                error("Creating FirModuleResolveState is not yet supported for $moduleInfo")
            }
            val sessionProvider = sessionProviderStorage.getSessionProvider(moduleInfo, configureSession)
            val firFileBuilder = sessionProvider.rootModuleSession.firFileBuilder
            return FirModuleResolveStateImpl(
                sessionProviderStorage.project,
                moduleInfo,
                sessionProvider,
                firFileBuilder,
                FirLazyDeclarationResolver(firFileBuilder),
            )
        }
    }
}

@TestOnly
fun createResolveStateForNoCaching(
    moduleInfo: ModuleInfo,
    project: Project,
    configureSession: (FirIdeSession.() -> Unit)? = null,
): FirModuleResolveState =
    FirIdeResolveStateService.createResolveStateFor(
        moduleInfo = moduleInfo,
        sessionProviderStorage = FirIdeSessionProviderStorage(project),
        configureSession = configureSession
    )


