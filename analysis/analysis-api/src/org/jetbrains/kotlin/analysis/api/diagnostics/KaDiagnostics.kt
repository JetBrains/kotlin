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
 * [KaDiagnostics] is obtained from [diagnostics]. By default, the query yields diagnostics of
 * [common checkers][KaDiagnosticCheckerKind.COMMON] which are not [suppressed][KaDiagnostic.isSuppressed]
 * at their use site recursively – in other words, exactly the diagnostics which the compiler reports:
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
 *     .ignoreSuppressed(true)
 *     .directOnly(true)
 *     .forEach { handle(it) }
 * ```
 *
 * #### Laziness
 *
 * [KaDiagnostics] is a [Sequence]:
 *
 * - Calling `diagnostics()` itself doesn't trigger code analysis. The operation is _intermediate_ and _stateless_.
 * - Diagnostics are computed on-demand as the sequence is iterated over.
 * - You can iterate over the same sequence multiple times.
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
     * Returns a [KaDiagnostics] which yields [suppressed][KaDiagnostic.isSuppressed] diagnostics depending on the value of [ignore].
     *
     * Suppressed diagnostics are not reported by the compiler, so they should not be presented to the user as is. They are useful for
     * tooling which analyzes suppressions themselves, such as an inspection which detects redundant `@Suppress` annotations.
     *
     * Collecting suppressed diagnostics does not require additional analysis.
     *
     * The default behavior is `true`.
     */
    public fun ignoreSuppressed(ignore: Boolean): KaDiagnostics

    /**
     * Returns a [KaDiagnostics] which yields diagnostics only on the given [KtElement] itself depending on [direct] flag.
     *
     * If [direct] is `true`, [KaDiagnosticWithPsi] of the element's children are **not** included, so the result is not the complete set
     * of diagnostics which concern the element: a diagnostic about the element might be reported on one of its children,
     * or on a containing element. Prefer `false` unless the diagnostics of the exact element are required.
     *
     * The default behavior is `false`.
     */
    public fun directOnly(direct: Boolean): KaDiagnostics
}
