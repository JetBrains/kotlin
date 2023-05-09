/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.retryOnInvalidSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.*
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class LLFirScriptResolveSession(
    useSiteKtModule: KtModule,
    useSiteSessionFactory: (KtModule) -> LLFirSession
) : LLFirResolvableResolveSession(useSiteKtModule, useSiteSessionFactory) {
    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        retryOnInvalidSession {
            val moduleComponents = getModuleComponentsForElement(element)
            return moduleComponents.diagnosticsCollector.getDiagnosticsFor(element, filter)
        }
    }

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> {
        retryOnInvalidSession {
            val moduleComponents = getModuleComponentsForElement(ktFile)
            return moduleComponents.diagnosticsCollector.collectDiagnosticsForFile(ktFile, filter)
        }
    }

    override fun getModuleKind(module: KtModule): ModuleKind {
        return when (module) {
            useSiteKtModule, is KtSourceModule -> ModuleKind.RESOLVABLE_MODULE
            is KtBuiltinsModule, is KtLibraryModule -> ModuleKind.BINARY_MODULE
            else -> unexpectedElementError("module", module)
        }
    }
}