/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaObsoleteComponentApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.psi.KtFile

@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaDiagnosticProvider : KaSessionComponent {
    /**
     * Collects all diagnostics for the given file.
     *
     * Eager version of [diagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics].
     *
     * @see org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics
     */
    public fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>

}

/**
 * [KaDiagnosticCheckerFilter] controls which kinds of diagnostics are included in the result of diagnostic collection.
 */
@KaObsoleteComponentApi
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
     * Includes diagnostics from experimental checkers.
     *
     * Their role is to be run in the IDE similar to [ONLY_EXTENDED_CHECKERS] with the following differences:
     * * They might have false positives
     * * They might be slow
     */
    ONLY_EXPERIMENTAL_CHECKERS,

    /**
     * Includes diagnostics from both common and extended checkers.
     */
    EXTENDED_AND_COMMON_CHECKERS,
}

/**
 * Collects all diagnostics for the given file.
 *
 * Eager version of [diagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics].
 *
 * @see org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics
 */
@KaObsoleteComponentApi
context(session: KaSession)
public fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> {
    return with(session) {
        collectDiagnostics(
            filter = filter,
        )
    }
}
