/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.analysis.api.projectStructure.KaBuiltinsModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaDanglingFileModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibrarySourceModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaNotUnderContentRootModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLEmptyDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleResolutionStrategy
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleResolutionStrategyProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLSessionProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLSimpleResolutionStrategyProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLSourceDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

@LLFirInternals
class LLFirResolveSessionService(project: Project) {
    private val cache = LLFirSessionCache.getInstance(project)

    fun getFirResolveSession(module: KaModule): LLFirResolveSession {
        return create(module, cache::getSession)
    }

    @TestOnly
    fun getFirResolveSessionForBinaryModule(module: KaModule): LLFirResolveSession {
        return create(module) { cache.getSession(it, true) }
    }

    fun getFirResolveSessionNoCaching(module: KaModule): LLFirResolveSession {
        return create(module, cache::getSessionNoCaching)
    }

    private fun create(module: KaModule, factory: (KaModule) -> LLFirSession): LLFirResolvableResolveSession {
        val moduleProvider = LLModuleProvider(module)
        val sessionProvider = LLSessionProvider(module, factory)
        val resolutionStrategyProvider = createResolutionStrategyProvider(module, moduleProvider)
        val diagnosticProvider = createDiagnosticProvider(moduleProvider, sessionProvider)

        return LLFirResolvableResolveSession(moduleProvider, resolutionStrategyProvider, sessionProvider, diagnosticProvider)
    }

    private fun createResolutionStrategyProvider(module: KaModule, moduleProvider: LLModuleProvider): LLModuleResolutionStrategyProvider {
        return when (module) {
            is KaSourceModule -> LLSourceModuleResolutionStrategyProvider
            is KaLibraryModule, is KaBuiltinsModule, is KaLibrarySourceModule -> LLBinaryModuleResolutionStrategyProvider(module)
            is KaScriptModule -> LLScriptModuleResolutionStrategyProvider(module)
            is KaDanglingFileModule -> {
                val contextModule = module.contextModule
                val contextResolutionStrategyProvider = createResolutionStrategyProvider(contextModule, moduleProvider)
                LLDanglingFileResolutionStrategyProvider(contextResolutionStrategyProvider)
            }
            is KaNotUnderContentRootModule -> LLSimpleResolutionStrategyProvider(module)
            else -> {
                errorWithFirSpecificEntries("Unexpected ${module::class.java}") {
                    withEntry("module", module) { it.moduleDescription }
                }
            }
        }
    }

    private fun createDiagnosticProvider(moduleProvider: LLModuleProvider, sessionProvider: LLSessionProvider): LLDiagnosticProvider {
        return when (moduleProvider.useSiteModule) {
            is KaSourceModule,
            is KaScriptModule,
            is KaDanglingFileModule
                -> LLSourceDiagnosticProvider(moduleProvider, sessionProvider)
            else -> LLEmptyDiagnosticProvider
        }
    }

    companion object {
        fun getInstance(project: Project): LLFirResolveSessionService =
            project.getService(LLFirResolveSessionService::class.java)
    }
}

private object LLSourceModuleResolutionStrategyProvider : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KaModule): LLModuleResolutionStrategy {
        return when (module) {
            is KaSourceModule -> LLModuleResolutionStrategy.LAZY
            is KaBuiltinsModule, is KaLibraryModule -> LLModuleResolutionStrategy.STATIC
            else -> unexpectedElementError("module", module)
        }
    }
}


private class LLBinaryModuleResolutionStrategyProvider(private val useSiteModule: KaModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KaModule): LLModuleResolutionStrategy {
        LLFirLibraryOrLibrarySourceResolvableModuleSession.checkIsValidKtModule(module)
        return if (module == useSiteModule) LLModuleResolutionStrategy.LAZY else LLModuleResolutionStrategy.STATIC
    }
}

private class LLScriptModuleResolutionStrategyProvider(private val useSiteModule: KaModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KaModule): LLModuleResolutionStrategy {
        return when (module) {
            useSiteModule, is KaSourceModule, is KaLibrarySourceModule -> LLModuleResolutionStrategy.LAZY
            is KaBuiltinsModule, is KaLibraryModule -> LLModuleResolutionStrategy.STATIC
            else -> unexpectedElementError("module", module)
        }
    }
}

private class LLDanglingFileResolutionStrategyProvider(private val delegate: LLModuleResolutionStrategyProvider) :
    LLModuleResolutionStrategyProvider {
    override fun getKind(module: KaModule): LLModuleResolutionStrategy {
        return when (module) {
            is KaDanglingFileModule -> LLModuleResolutionStrategy.LAZY
            else -> delegate.getKind(module)
        }
    }
}