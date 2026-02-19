/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

interface LLDiagnosticProvider {
    /**
     * Returns all compiler diagnostics for the [file], matching the [filter].
     */
    fun collectDiagnostics(file: KtFile, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic>

    /**
     * Returns all compiler diagnostics for the specific [element], matching the [filter].
     * This function is not recursive; diagnostics for nested elements are not returned.
     */
    fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic>
}

internal object LLEmptyDiagnosticProvider : LLDiagnosticProvider {
    override fun collectDiagnostics(file: KtFile, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        return emptyList()
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        return emptyList()
    }
}

internal class LLSourceDiagnosticProvider(
    private val moduleProvider: LLModuleProvider,
    private val sessionProvider: LLSessionProvider
) : LLDiagnosticProvider {
    override fun collectDiagnostics(file: KtFile, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        val module = moduleProvider.getModule(file)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.collectDiagnosticsForFile(file, filter)
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<KtPsiDiagnostic> {
        val module = moduleProvider.getModule(element)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.getDiagnosticsFor(element, filter)
    }
}