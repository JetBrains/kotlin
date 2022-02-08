/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.DiagnosticsCollector
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.builder.FirFileBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazy.resolve.FirLazyDeclarationResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionProvider
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.analysis.project.structure.KtSourceModule
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

internal class LLFirSourceModuleResolveState(
    override val project: Project,
    override val module: KtModule,
    sessionProvider: LLFirSessionProvider,
    firFileBuilder: FirFileBuilder,
    firLazyDeclarationResolver: FirLazyDeclarationResolver,
) : LLFirResolvableModuleResolveState(sessionProvider, firFileBuilder, firLazyDeclarationResolver) {
    private val diagnosticsCollector = DiagnosticsCollector(fileStructureCache, cache)

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> =
        diagnosticsCollector.getDiagnosticsFor(element, filter)

    override fun collectDiagnosticsForFile(ktFile: KtFile, filter: DiagnosticCheckerFilter): Collection<KtPsiDiagnostic> =
        diagnosticsCollector.collectDiagnosticsForFile(ktFile, filter)

    override fun getModuleKind(module: KtModule): ModuleKind {
        return when (module) {
            is KtSourceModule -> ModuleKind.RESOLVABLE_MODULE
            is KtLibraryModule -> ModuleKind.BINARY_MODULE
            else -> unexpectedElementError("module", module)
        }
    }
}

