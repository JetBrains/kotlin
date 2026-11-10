/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.diagnostics

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.psi.KtElement

/**
 * A description of a diagnostic query, which yields the requested [diagnostics][KaDiagnosticWithPsi] on iteration.
 *
 * [KaDiagnostics] is obtained from [diagnostics]. By default, the query yields the diagnostics of
 * [common checkers][KaDiagnosticCheckerKind.COMMON] reported on the element and on all its descendants, except for
 * [suppressed][KaDiagnostic.isSuppressed] ones – in other words, exactly the diagnostics which the compiler reports there:
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
 * element.diagnostics()
 *     .withCheckers(KaDiagnosticCheckerKind.COMMON, KaDiagnosticCheckerKind.EXTENDED)
 *     .ignoreSuppressed(false)
 *     .directOnly(true)
 *     .forEach { handle(it) }
 * ```
 *
 * #### Laziness
 *
 * [KaDiagnostics] is a [Sequence]:
 *
 * - Calling `diagnostics()` and the modifiers below doesn't trigger code analysis. The operations are _intermediate_ and _stateless_.
 * - Diagnostics are computed on-demand as the sequence is iterated over, so a short-circuiting operation such as [Sequence.any]
 *   may stop before the whole scope is analyzed.
 * - You can iterate over the same sequence multiple times.
 * - The diagnostics order is unspecified.
 *
 * #### Immutability
 *
 * The query is immutable, so it can be stored and adjusted in different ways:
 *
 * ```kotlin
 * val diagnostics = element.diagnostics()
 * val direct = diagnostics.directOnly(true) // 'diagnostics' still covers the whole subtree
 * ```
 *
 * Like other [lifetime owners][KaLifetimeOwner], a query must not be used outside the analysis session it was obtained from.
 *
 * @see diagnostics
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDiagnostics : KaLifetimeOwner, Sequence<KaDiagnosticWithPsi<*>> {
    /**
     * Returns a [KaDiagnostics] which yields diagnostics of the given checker [kinds].
     *
     * The [kinds] **replace** the currently requested kinds, they are not added to them. An empty [kinds] set results in no diagnostics, and
     * no checkers are run in that case.
     *
     * By default, only [common checkers][KaDiagnosticCheckerKind.COMMON] are run.
     */
    public fun withCheckers(kinds: Set<KaDiagnosticCheckerKind>): KaDiagnostics

    /**
     * Returns a [KaDiagnostics] which yields diagnostics of the given checker [kinds].
     *
     * The [kinds] **replace** the currently requested kinds, they are not added to them. Passing no kinds results in no diagnostics, and no
     * checkers are run in that case.
     *
     * By default, only [common checkers][KaDiagnosticCheckerKind.COMMON] are run.
     */
    public fun withCheckers(vararg kinds: KaDiagnosticCheckerKind): KaDiagnostics

    /**
     * Returns a [KaDiagnostics] which skips [suppressed][KaDiagnostic.isSuppressed] diagnostics if [ignore] is `true`, or yields them as
     * well if [ignore] is `false`.
     *
     * Suppressed diagnostics are not reported by the compiler, so they should not be presented to the user as is. They are useful for
     * tooling which analyzes suppressions themselves, such as an inspection which detects redundant `@Suppress` annotations. A client can
     * tell them apart from the reported diagnostics by [KaDiagnostic.isSuppressed].
     *
     * Collecting suppressed diagnostics does not require additional analysis.
     *
     * The default behavior is `true`.
     */
    public fun ignoreSuppressed(ignore: Boolean): KaDiagnostics

    /**
     * Returns a [KaDiagnostics] which yields only the diagnostics reported on the [KtElement] itself if [direct] is `true`, or the
     * diagnostics reported on the element and on all its descendants if [direct] is `false`.
     *
     * A direct-only result is not the complete set of diagnostics which concern the element: a diagnostic about the element might be
     * reported on one of its children. Prefer `false` unless the diagnostics of the exact element are required.
     *
     * The default behavior is `false`.
     */
    public fun directOnly(direct: Boolean): KaDiagnostics
}
