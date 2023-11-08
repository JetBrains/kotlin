/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleResolutionStrategy
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLEmptyDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleResolutionStrategyProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLSimpleResolutionStrategyProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLSourceDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

class LLFirResolveSessionService(project: Project) {
    private val cache = LLFirSessionCache.getInstance(project)

    fun getFirResolveSession(module: KtModule): LLFirResolveSession {
        return create(module, cache::getSession)
    }

    @TestOnly
    fun getFirResolveSessionForBinaryModule(module: KtModule): LLFirResolveSession {
        return create(module) { cache.getSession(it, true) }
    }

    fun getFirResolveSessionNoCaching(module: KtModule): LLFirResolveSession {
        return create(module, cache::getSessionNoCaching)
    }

    private fun create(module: KtModule, factory: (KtModule) -> LLFirSession): LLFirResolvableResolveSession {
        val moduleProvider = LLModuleProvider(module)
        val sessionProvider = LLSessionProvider(module, factory)
        val resolutionStrategyProvider = createResolutionStrategyProvider(module, moduleProvider)
        val diagnosticProvider = createDiagnosticProvider(moduleProvider, sessionProvider)

        return LLFirResolvableResolveSession(moduleProvider, resolutionStrategyProvider, sessionProvider, diagnosticProvider)
    }

    private fun createResolutionStrategyProvider(module: KtModule, moduleProvider: LLModuleProvider): LLModuleResolutionStrategyProvider {
        return when (module) {
            is KtSourceModule -> LLSourceModuleResolutionStrategyProvider
            is KtLibraryModule, is KtLibrarySourceModule -> LLLibraryModuleResolutionStrategyProvider(module)
            is KtScriptModule -> LLScriptModuleResolutionStrategyProvider(module)
            is KtDanglingFileModule -> {
                val contextModule = module.contextModule
                val contextResolutionStrategyProvider = createResolutionStrategyProvider(contextModule, moduleProvider)
                LLDanglingFileResolutionStrategyProvider(contextResolutionStrategyProvider)
            }
            is KtNotUnderContentRootModule -> LLSimpleResolutionStrategyProvider(module)
            else -> {
                errorWithFirSpecificEntries("Unexpected ${module::class.java}") {
                    withEntry("module", module) { it.moduleDescription }
                }
            }
        }
    }

    private fun createDiagnosticProvider(moduleProvider: LLModuleProvider, sessionProvider: LLSessionProvider): LLDiagnosticProvider {
        return when (moduleProvider.useSiteModule) {
            is KtSourceModule,
            is KtScriptModule,
            is KtDanglingFileModule -> LLSourceDiagnosticProvider(moduleProvider, sessionProvider)
            else -> LLEmptyDiagnosticProvider
        }
    }

    companion object {
        fun getInstance(project: Project): LLFirResolveSessionService =
            project.getService(LLFirResolveSessionService::class.java)
    }
}

private object LLSourceModuleResolutionStrategyProvider : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KtModule): LLModuleResolutionStrategy {
        return when (module) {
            is KtSourceModule -> LLModuleResolutionStrategy.LAZY
            is KtBuiltinsModule, is KtLibraryModule -> LLModuleResolutionStrategy.STATIC
            else -> unexpectedElementError("module", module)
        }
    }
}

private class LLLibraryModuleResolutionStrategyProvider(private val useSiteModule: KtModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KtModule): LLModuleResolutionStrategy {
        LLFirLibraryOrLibrarySourceResolvableModuleSession.checkIsValidKtModule(module)
        return if (module == useSiteModule) LLModuleResolutionStrategy.LAZY else LLModuleResolutionStrategy.STATIC
    }
}

private class LLScriptModuleResolutionStrategyProvider(private val useSiteModule: KtModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KtModule): LLModuleResolutionStrategy {
        return when (module) {
            useSiteModule, is KtSourceModule -> LLModuleResolutionStrategy.LAZY
            is KtBuiltinsModule, is KtLibraryModule -> LLModuleResolutionStrategy.STATIC
            else -> unexpectedElementError("module", module)
        }
    }
}

private class LLDanglingFileResolutionStrategyProvider(private val delegate: LLModuleResolutionStrategyProvider) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KtModule): LLModuleResolutionStrategy {
        return when (module) {
            is KtDanglingFileModule -> LLModuleResolutionStrategy.LAZY
            else -> delegate.getKind(module)
        }
    }
}