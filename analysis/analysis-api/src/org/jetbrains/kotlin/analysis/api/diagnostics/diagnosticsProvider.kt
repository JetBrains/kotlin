/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.diagnostics

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.psi.KtElement

/**
 * Returns the [diagnostics][KaDiagnostics] reported on the given [KtElement] and on all its children.
 *
 * The result is computed lazily, and only as far as it is iterated. See [KaDiagnostics] for the defaults and for the ways to adjust them.
 *
 * #### Example
 *
 * ```kotlin
 * for (diagnostic in file.diagnostics()) {
 *     handle(diagnostic)
 * }
 * ```
 *
 * @see directDiagnostics
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtElement.diagnostics(): KaDiagnostics {
    @OptIn(KaImplementationDetail::class)
    return internals.diagnosticProvider.diagnostics(this, isRecursive = true)
}

/**
 * Returns the [diagnostics][KaDiagnostics] reported on the given [KtElement] itself.
 *
 * Diagnostics of the element's children are **not** included, so the result is not the complete set of diagnostics which concern the
 * element: a diagnostic about the element might be reported on one of its children, or on a containing element. Prefer [diagnostics] unless
 * the diagnostics of the exact element are required.
 *
 * @see diagnostics
 */
@KaExperimentalApi
context(session: KaSession)
public fun KtElement.directDiagnostics(): KaDiagnostics {
    @OptIn(KaImplementationDetail::class)
    return internals.diagnosticProvider.diagnostics(this, isRecursive = false)
}
