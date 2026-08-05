/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.diagnostics

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner

/**
 * A description of a diagnostic query, which yields the requested [diagnostics][KaDiagnosticWithPsi] on iteration.
 *
 * [KaDiagnostics] is obtained from [diagnostics] or [directDiagnostics]. By default, the query yields diagnostics of
 * [common checkers][KaDiagnosticCheckerKind.COMMON] which are not [suppressed][KaDiagnostic.isSuppressed] at their use site – in other
 * words, exactly the diagnostics which the compiler reports:
 *
 * ```kotlin
 * for (diagnostic in file.diagnostics()) {
 *     handle(diagnostic)
 * }
 * ```
 *
 * The default can be adjusted before the iteration begins:
 *
 * ```kotlin
 * file.diagnostics()
 *     .withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)
 *     .includingSuppressed()
 *     .forEach { handle(it) }
 * ```
 *
 * #### Laziness
 *
 * Creating a [KaDiagnostics] and applying its modifiers does no work: diagnostics are computed during the iteration, and only as far as the
 * iteration goes. Therefore, short-circuiting operations such as [any][kotlin.sequences.any] or [first][kotlin.sequences.first] do not have
 * to analyze the whole requested scope.
 *
 * Unlike a general [Sequence], [KaDiagnostics] can be iterated multiple times. Repeated iteration recomputes the result, although the
 * underlying analysis is usually cached.
 *
 * @see diagnostics
 * @see directDiagnostics
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDiagnostics : KaLifetimeOwner, Sequence<KaDiagnosticWithPsi<*>> {
    /**
     * Returns a [KaDiagnostics] which yields diagnostics of the given checker [kinds].
     *
     * The [kinds] **replace** the currently requested kinds, they are not added to them. An empty [kinds] set results in no diagnostics, and
     * no checkers are run in that case.
     */
    public fun withCheckers(kinds: Set<KaDiagnosticCheckerKind>): KaDiagnostics

    /**
     * Returns a [KaDiagnostics] which yields diagnostics of the given checker [kinds].
     *
     * The [kinds] **replace** the currently requested kinds, they are not added to them. Passing no kinds results in no diagnostics, and no
     * checkers are run in that case.
     */
    public fun withCheckers(vararg kinds: KaDiagnosticCheckerKind): KaDiagnostics

    /**
     * Returns a [KaDiagnostics] which also yields diagnostics that are [suppressed][KaDiagnostic.isSuppressed] at their use site, e.g., by a
     * `@Suppress` annotation.
     *
     * Suppressed diagnostics are not reported by the compiler, so they should not be presented to the user as is. They are useful for
     * tooling which analyzes suppressions themselves, such as an inspection which detects redundant `@Suppress` annotations.
     *
     * Collecting suppressed diagnostics does not require additional analysis. The function is idempotent.
     *
     * @see excludingSuppressed
     */
    public fun includingSuppressed(): KaDiagnostics

    /**
     * Returns a [KaDiagnostics] which does not yield diagnostics that are [suppressed][KaDiagnostic.isSuppressed] at their use site.
     *
     * This is the default behavior, so the function is only needed to undo [includingSuppressed], e.g., when a query is built elsewhere.
     * The function is idempotent.
     *
     * @see includingSuppressed
     */
    public fun excludingSuppressed(): KaDiagnostics
}
