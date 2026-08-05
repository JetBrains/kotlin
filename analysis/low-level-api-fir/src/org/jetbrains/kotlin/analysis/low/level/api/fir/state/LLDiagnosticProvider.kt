/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.state

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLDiagnostic
import org.jetbrains.kotlin.psi.KtElement

@KaImplementationDetail
interface LLDiagnosticProvider {
    /**
     * Returns all compiler diagnostics reported on the [element] and on its children, matching the [filter].
     */
    fun diagnostics(element: KtElement, filter: DiagnosticCheckerFilter): Sequence<LLDiagnostic>

    /**
     * Returns all compiler diagnostics for the specific [element], matching the [filter].
     * This function is not recursive; diagnostics for nested elements are not returned.
     */
    fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<LLDiagnostic>
}

internal object LLEmptyDiagnosticProvider : LLDiagnosticProvider {
    override fun diagnostics(element: KtElement, filter: DiagnosticCheckerFilter): Sequence<LLDiagnostic> {
        return emptySequence()
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<LLDiagnostic> {
        return emptyList()
    }
}

internal class LLSourceDiagnosticProvider(
    private val moduleProvider: LLModuleProvider,
    private val sessionProvider: LLSessionProvider,
) : LLDiagnosticProvider {
    override fun diagnostics(element: KtElement, filter: DiagnosticCheckerFilter): Sequence<LLDiagnostic> {
        val module = moduleProvider.getModule(element)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.diagnosticsFor(element, filter)
    }

    override fun getDiagnostics(element: KtElement, filter: DiagnosticCheckerFilter): List<LLDiagnostic> {
        val module = moduleProvider.getModule(element)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.getDiagnosticsFor(element, filter)
    }
}
