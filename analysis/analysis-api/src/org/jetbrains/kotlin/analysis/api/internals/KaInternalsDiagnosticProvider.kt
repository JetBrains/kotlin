/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@OptIn(KaExperimentalApi::class)
@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaInternalsDiagnosticProvider {
    /**
     * Creates a [KaDiagnostics] query for the [element].
     *
     * @param isRecursive Whether diagnostics reported on the element's children are included.
     */
    public fun diagnostics(element: KtElement, isRecursive: Boolean): KaDiagnostics

    public fun directDiagnostics(element: KtElement, filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>

    public fun collectDiagnostics(file: KtFile, filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>

    public fun diagnostics(file: KtFile, filter: KaDiagnosticCheckerFilter): Sequence<KaDiagnosticWithPsi<*>>

    public fun diagnosticsIgnoringSuppression(file: KtFile, filter: KaDiagnosticCheckerFilter): Sequence<KaDiagnosticWithPsi<*>>
}
