/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.projectStructure.*
import org.jetbrains.kotlin.analysis.api.utils.errors.withKaModuleEntry
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirLibraryOrLibrarySourceResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.errorWithFirSpecificEntries
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

@LLFirInternals
class LLFirResolveSessionService(project: Project) {
    private val cache = LLFirSessionCache.getInstance(project)

    fun getFirResolveSession(module: KaModule): LLFirResolveSession {
        return create(module, cache::getSession)
    }

    private fun create(module: KaModule, factory: (KaModule) -> LLFirSession): LLFirResolveSession {
        val moduleProvider = LLModuleProvider(module)
        val sessionProvider = LLSessionProvider(module, factory)
        val resolutionStrategyProvider = createResolutionStrategyProvider(module, moduleProvider)
        val diagnosticProvider = createDiagnosticProvider(moduleProvider, sessionProvider)

        return LLFirResolveSession(moduleProvider, resolutionStrategyProvider, sessionProvider, diagnosticProvider)
    }

    private fun createResolutionStrategyProvider(module: KaModule, moduleProvider: LLModuleProvider): LLModuleResolutionStrategyProvider {
        return when (module) {
            is KaSourceModule -> LLSourceModuleResolutionStrategyProvider(module)
            is KaLibraryModule, is KaBuiltinsModule, is KaLibrarySourceModule -> LLBinaryModuleResolutionStrategyProvider(module)
            is KaScriptModule -> LLScriptModuleResolutionStrategyProvider(module)
            is KaDanglingFileModule -> {
                val contextModule = module.contextModule
                val contextResolutionStrategyProvider = createResolutionStrategyProvider(contextModule, moduleProvider)
                LLDanglingFileResolutionStrategyProvider(contextResolutionStrategyProvider)
            }
            is KaNotUnderContentRootModule -> LLSimpleResolutionStrategyProvider(module)
            else -> {
                errorWithFirSpecificEntries(
                    "`${module::class.java}` does not have a corresponding resolution strategy (resolvable: ${module.isResolvable}).",
                ) {
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

private class LLSourceModuleResolutionStrategyProvider(private val useSiteModule: KaModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KaModule): LLModuleResolutionStrategy {
        return when (module) {
            is KaSourceModule -> LLModuleResolutionStrategy.LAZY
            is KaBuiltinsModule, is KaLibraryModule -> LLModuleResolutionStrategy.STATIC
            else -> cannotProvideResolutionStrategy(module, useSiteModule)
        }
    }
}

private class LLBinaryModuleResolutionStrategyProvider(private val useSiteModule: KaModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KaModule): LLModuleResolutionStrategy {
        LLFirLibraryOrLibrarySourceResolvableModuleSession.checkIsValidKtModule(module)
        // Providing `LLModuleResolutionStrategy.LAZY` strategy for `KaLibrarySourceModule` is a workaround,
        // as `KaLibrarySourceModule` should not be used as dependencies.
        // It was added after including the project library scope
        // in resolution scopes of all `KaLibrarySourceModule`s and `KaLibraryModule`s.
        // See KT-75838
        return if (module == useSiteModule || module is KaLibrarySourceModule) LLModuleResolutionStrategy.LAZY else LLModuleResolutionStrategy.STATIC
    }
}

private class LLScriptModuleResolutionStrategyProvider(private val useSiteModule: KaModule) : LLModuleResolutionStrategyProvider {
    override fun getKind(module: KaModule): LLModuleResolutionStrategy {
        return when (module) {
            useSiteModule, is KaSourceModule, is KaLibrarySourceModule -> LLModuleResolutionStrategy.LAZY
            is KaBuiltinsModule, is KaLibraryModule -> LLModuleResolutionStrategy.STATIC
            else -> cannotProvideResolutionStrategy(module, useSiteModule)
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

private fun cannotProvideResolutionStrategy(module: KaModule, useSiteModule: KaModule): Nothing {
    errorWithAttachment("Cannot provide a resolution strategy for `${module::class.simpleName}`.") {
        withKaModuleEntry("module", module)
        withKaModuleEntry("useSiteModule", useSiteModule)
    }
}
