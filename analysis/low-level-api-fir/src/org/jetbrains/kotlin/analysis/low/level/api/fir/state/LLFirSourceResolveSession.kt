/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.project.structure.KtBuiltinsModule
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class LLFirSourceResolveSession(
    useSiteKtModule: KtModule,
    useSiteSessionFactory: (KtModule) -> LLFirSession
) : LLFirResolvableResolveSession(useSiteKtModule, useSiteSessionFactory) {
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
            is KtSourceModule -> ModuleKind.RESOLVABLE_MODULE
            is KtBuiltinsModule,
            is KtLibraryModule -> ModuleKind.BINARY_MODULE
            else -> unexpectedElementError("module", module)
        }
    }
}

