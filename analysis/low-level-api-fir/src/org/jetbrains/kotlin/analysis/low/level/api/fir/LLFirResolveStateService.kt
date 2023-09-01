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
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.KtModuleKind
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLEmptyDiagnosticProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLFirResolvableResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleKindProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.state.LLModuleProvider
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
        val moduleKindProvider = createModuleKindProvider(module)
        val diagnosticProvider = createDiagnosticProvider(moduleProvider, sessionProvider)

        return LLFirResolvableResolveSession(moduleProvider, moduleKindProvider, sessionProvider, diagnosticProvider)
    }

    private fun createModuleKindProvider(module: KtModule): LLModuleKindProvider {
        return when (module) {
            is KtSourceModule -> LLSourceModuleKindProvider
            is KtLibraryModule, is KtLibrarySourceModule -> LLLibraryModuleKindProvider(module)
            is KtScriptModule -> LLScriptModuleKindProvider(module)
            is KtNotUnderContentRootModule -> LLNotUnderContentRootModuleKindProvider(module)
            else -> {
                errorWithFirSpecificEntries("Unexpected ${module::class.java}") {
                    withEntry("module", module) { it.moduleDescription }
                }
            }
        }
    }

    private fun createDiagnosticProvider(moduleProvider: LLModuleProvider, sessionProvider: LLSessionProvider): LLDiagnosticProvider {
        return when (moduleProvider.useSiteModule) {
            is KtSourceModule, is KtScriptModule -> LLSourceDiagnosticProvider(moduleProvider, sessionProvider)
            else -> LLEmptyDiagnosticProvider
        }
    }

    companion object {
        fun getInstance(project: Project): LLFirResolveSessionService =
            project.getService(LLFirResolveSessionService::class.java)
    }
}

private object LLSourceModuleKindProvider : LLModuleKindProvider {
    override fun getKind(module: KtModule): KtModuleKind {
        return when (module) {
            is KtSourceModule -> KtModuleKind.RESOLVABLE_MODULE
            is KtBuiltinsModule, is KtLibraryModule -> KtModuleKind.BINARY_MODULE
            else -> unexpectedElementError("module", module)
        }
    }
}

private class LLLibraryModuleKindProvider(private val useSiteModule: KtModule) : LLModuleKindProvider {
    override fun getKind(module: KtModule): KtModuleKind {
        LLFirLibraryOrLibrarySourceResolvableModuleSession.checkIsValidKtModule(module)
        return if (module == useSiteModule) KtModuleKind.RESOLVABLE_MODULE else KtModuleKind.BINARY_MODULE
    }
}

private class LLScriptModuleKindProvider(private val useSiteModule: KtModule) : LLModuleKindProvider {
    override fun getKind(module: KtModule): KtModuleKind {
        return when (module) {
            useSiteModule, is KtSourceModule -> KtModuleKind.RESOLVABLE_MODULE
            is KtBuiltinsModule, is KtLibraryModule -> KtModuleKind.BINARY_MODULE
            else -> unexpectedElementError("module", module)
        }
    }
}

private class LLNotUnderContentRootModuleKindProvider(private val useSiteModule: KtModule) : LLModuleKindProvider {
    override fun getKind(module: KtModule): KtModuleKind {
        return when (module) {
            useSiteModule -> KtModuleKind.RESOLVABLE_MODULE
            else -> KtModuleKind.BINARY_MODULE
        }
    }
}