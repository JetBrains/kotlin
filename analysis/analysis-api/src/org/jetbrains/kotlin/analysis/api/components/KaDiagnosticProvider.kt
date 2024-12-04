/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

public interface KaDiagnosticProvider : KaSessionComponent {
    /**
     * Collects diagnostics for the given element.
     *
     * **Caution:** The result might not include diagnostics that are reported for child elements, as well as diagnostics provided by the
     * checkers of containing elements. Therefore, the API might not return all expected diagnostics for an element.
     * [KtFile.collectDiagnostics] should be preferred at the current time.
     */
    @KaExperimentalApi
    public fun KtElement.diagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>

    /**
     * Collects all diagnostics for the given file.
     */
    public fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>
}

/**
 * [KaDiagnosticCheckerFilter] controls which kinds of diagnostics are included in the result of diagnostic collection.
 */
public enum class KaDiagnosticCheckerFilter {
    /**
     * Includes diagnostics only from the compiler's common checkers.
     */
    ONLY_COMMON_CHECKERS,

    /**
     * Includes diagnostics from extended checkers (that typically run only in the IDE).
     */
    ONLY_EXTENDED_CHECKERS,

    /**
     * Includes diagnostics from both common and extended checkers.
     */
    EXTENDED_AND_COMMON_CHECKERS,
}
