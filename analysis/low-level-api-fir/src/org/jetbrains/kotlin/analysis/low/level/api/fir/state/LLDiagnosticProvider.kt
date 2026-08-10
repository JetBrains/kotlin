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
     * Returns all compiler diagnostics reported on the [element], matching the [filter].
     *
     * @param isRecursive Whether diagnostics reported on the element's children are included as well.
     */
    fun diagnostics(element: KtElement, filter: DiagnosticCheckerFilter, isRecursive: Boolean): Sequence<LLDiagnostic>
}

internal object LLEmptyDiagnosticProvider : LLDiagnosticProvider {
    override fun diagnostics(element: KtElement, filter: DiagnosticCheckerFilter, isRecursive: Boolean): Sequence<LLDiagnostic> {
        return emptySequence()
    }
}

internal class LLSourceDiagnosticProvider(
    private val moduleProvider: LLModuleProvider,
    private val sessionProvider: LLSessionProvider,
) : LLDiagnosticProvider {
    override fun diagnostics(element: KtElement, filter: DiagnosticCheckerFilter, isRecursive: Boolean): Sequence<LLDiagnostic> {
        val module = moduleProvider.getModule(element)
        val moduleComponents = sessionProvider.getResolvableSession(module).moduleComponents
        return moduleComponents.diagnosticsCollector.diagnostics(element, filter, isRecursive)
    }
}
