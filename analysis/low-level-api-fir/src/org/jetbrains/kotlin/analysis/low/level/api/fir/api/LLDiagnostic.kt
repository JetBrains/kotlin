/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic

/**
 * A [diagnostic] reported by compiler checkers, together with its [suppression status][isSuppressed].
 *
 * Diagnostics are collected regardless of suppression: the collector reports every diagnostic and marks those which are suppressed at
 * their use site. This way both suppressed and non-suppressed diagnostics become available after a single collection pass.
 *
 * @see org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics.LLFirDiagnosticReporter
 */
@KaImplementationDetail
class LLDiagnostic(
    val diagnostic: KtPsiDiagnostic,

    /**
     * Whether the diagnostic is suppressed at its use site, e.g., by a `@Suppress` annotation.
     *
     * Suppressed diagnostics are not reported by the compiler. They are still collected, as they are useful for tooling which analyzes
     * suppressions themselves.
     */
    val isSuppressed: Boolean,
) {
    override fun toString(): String = if (isSuppressed) "$diagnostic (suppressed)" else diagnostic.toString()
}
