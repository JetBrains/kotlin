/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.util.containers.CollectionFactory
import com.intellij.util.containers.FactoryMap
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirGlobalResolveComponents
import org.jetbrains.kotlin.analysis.low.level.api.fir.LLFirResolveSessionService
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.*
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class LLFirScriptResolveSession(
    override val globalComponents: LLFirGlobalResolveComponents,
    override val project: Project,
    override val useSiteKtModule: KtModule,
    sessionProvider: LLFirSessionProvider
) : LLFirResolvableResolveSession(sessionProvider) {
    private val dependencyCache: Map<KtModule, LLFirSession> =
        FactoryMap.createMap(::createDependencySession, CollectionFactory::createConcurrentSoftValueMap)

    private fun createDependencySession(module: KtModule): LLFirSession {
        when (module) {
            is KtScriptDependencyModule -> {
                val resolveSessionService = ServiceManager.getService(project, LLFirResolveSessionService::class.java)
                return resolveSessionService.getFirResolveSession(module).useSiteFirSession
            }
            else -> error("Unsupported script dependency session type: ${module.javaClass.name}")
        }
    }

    override fun getSessionFor(module: KtModule): LLFirSession {
        if (module is KtScriptDependencyModule) {
            return dependencyCache.getValue(module)
        }

        return super.getSessionFor(module)
    }

    override fun getResolvableSessionFor(module: KtModule): LLFirResolvableModuleSession {
        if (module is KtScriptDependencyModule && module is KtLibrarySourceModule) {
            return dependencyCache[module] as LLFirResolvableModuleSession
        }

        return super.getResolvableSessionFor(module)
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        val moduleComponents = getModuleComponentsForElement(element)
        return moduleComponents.diagnosticsCollector.getDiagnosticsFor(element, filter)
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> {
        val moduleComponents = getModuleComponentsForElement(ktFile)
        return moduleComponents.diagnosticsCollector.collectDiagnosticsForFile(ktFile, filter)
    }

    override fun getModuleKind(module: KtModule): ModuleKind {
        return when (module) {
            useSiteKtModule, is KtSourceModule -> ModuleKind.RESOLVABLE_MODULE
            is KtBuiltinsModule, is KtLibraryModule -> ModuleKind.BINARY_MODULE
            else -> unexpectedElementError("module", module)
        }
    }
}