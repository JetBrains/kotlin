/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

public interface KaDiagnosticProvider {
    /**
     * Computes diagnostics for the given element.
     *
     * Note that the result may not include diagnostics that are reported for children elements,
     * as well as diagnostics provided by the containing element checkers.
     */
    @KaExperimentalApi
    public fun KtElement.diagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>

    @KaExperimentalApi
    @Deprecated("Use 'diagnostics()' instead.", replaceWith = ReplaceWith("diagnostic(filter)"))
    public fun KtElement.getDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> {
        return diagnostics(filter)
    }

    /**
     * Computes all diagnostics for the given file.
     */
    public fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>

    @Deprecated("Use 'collectDiagnostics()' instead.", replaceWith = ReplaceWith("collectDiagnostics(filter)"))
    public fun KtFile.collectDiagnosticsForFile(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> {
        return collectDiagnostics(filter)
    }
}

public enum class KaDiagnosticCheckerFilter {
    ONLY_COMMON_CHECKERS,
    ONLY_EXTENDED_CHECKERS,
    EXTENDED_AND_COMMON_CHECKERS,
}

@Deprecated("Use 'KaDiagnosticCheckerFilter' instead.", replaceWith = ReplaceWith("KaDiagnosticCheckerFilter"))
public typealias KtDiagnosticCheckerFilter = KaDiagnosticCheckerFilter