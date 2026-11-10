/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DeprecatedCallableAddReplaceWith", "DEPRECATION")

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
     * Deprecated: Use [diagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics] instead. The [filter] becomes a
     * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query, which is
     * lazy, so call `toList()` if a collection is needed:
     *
     * ```kotlin
     * file.diagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED).toList()
     * ```
     *
     * `KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS` is the default of the query, so `collectDiagnostics(ONLY_COMMON_CHECKERS)` becomes
     * just `diagnostics()`.
     *
     * @see org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics
     */
    @Deprecated(
        "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics' endpoint instead." +
                " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'." +
                " The query is lazy, so call 'toList()' if a collection is needed."
    )
    public fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>>
}

/**
 * [KaDiagnosticCheckerFilter] controls which kinds of diagnostics are included in the result of diagnostic collection.
 *
 * Deprecated: Use a set of [KaDiagnosticCheckerKind][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticCheckerKind]s with
 * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] instead:
 *
 * - [ONLY_COMMON_CHECKERS] – `COMMON`, the default of [diagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics]
 * - [ONLY_EXTENDED_CHECKERS] – `EXTENDED`
 * - [ONLY_EXPERIMENTAL_CHECKERS] – `EXPERIMENTAL`
 * - [EXTENDED_AND_COMMON_CHECKERS] – `COMMON` and `EXTENDED`
 */
@KaObsoleteComponentApi
@Deprecated(
    "Use a set of 'org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticCheckerKind' with 'KaDiagnostics.withCheckers' instead."
)
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
 * Deprecated: Use [diagnostics][org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics] instead. The [filter] becomes a
 * [withCheckers][org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics.withCheckers] modifier of the returned query, which is
 * lazy, so call `toList()` if a collection is needed:
 *
 * ```kotlin
 * file.diagnostics().withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED).toList()
 * ```
 *
 * `KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS` is the default of the query, so `collectDiagnostics(ONLY_COMMON_CHECKERS)` becomes
 * just `diagnostics()`.
 *
 * @see org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics
 */
@KaObsoleteComponentApi
@Deprecated(
    "Use the 'org.jetbrains.kotlin.analysis.api.diagnostics.diagnostics' endpoint instead." +
            " The 'filter' argument becomes a 'withCheckers' modifier of the returned 'KaDiagnostics'." +
            " The query is lazy, so call 'toList()' if a collection is needed."
)
context(session: KaSession)
public fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> {
    return with(session) {
        collectDiagnostics(
            filter = filter,
        )
    }
}
