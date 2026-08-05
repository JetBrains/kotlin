/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticCheckerKind
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsDiagnosticProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.diagnostics
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getDiagnostics
import org.jetbrains.kotlin.psi.KtElement

@OptIn(KaExperimentalApi::class)
internal class KaFirDiagnosticProvider(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaInternalsDiagnosticProvider, KaFirSessionComponent {
    override fun diagnostics(element: KtElement, isRecursive: Boolean): KaDiagnostics = element.withPsiValidityAssertion {
        KaFirDiagnostics(element, isRecursive, DEFAULT_CHECKER_KINDS, includeSuppressed = false)
    }

    /**
     * A [KaDiagnostics] query. The query is immutable: every modifier returns a new instance, so it can be safely reused and adjusted.
     */
    private inner class KaFirDiagnostics(
        private val element: KtElement,
        private val isRecursive: Boolean,
        private val checkerKinds: Set<KaDiagnosticCheckerKind>,
        private val includeSuppressed: Boolean,
    ) : KaDiagnostics {
        override val token: KaLifetimeToken get() = analysisSession.token

        override fun withCheckers(kinds: Set<KaDiagnosticCheckerKind>): KaDiagnostics = withValidityAssertion {
            if (kinds == checkerKinds) return this

            KaFirDiagnostics(element, isRecursive, kinds.toSet(), includeSuppressed)
        }

        override fun withCheckers(vararg kinds: KaDiagnosticCheckerKind): KaDiagnostics = withCheckers(kinds.toSet())

        override fun includingSuppressed(): KaDiagnostics = withValidityAssertion {
            if (includeSuppressed) return this

            KaFirDiagnostics(element, isRecursive, checkerKinds, includeSuppressed = true)
        }

        override fun excludingSuppressed(): KaDiagnostics = withValidityAssertion {
            if (!includeSuppressed) return this

            KaFirDiagnostics(element, isRecursive, checkerKinds, includeSuppressed = false)
        }

        override fun iterator(): Iterator<KaDiagnosticWithPsi<*>> = withValidityAssertion {
            // There is nothing to compute if no checkers were requested
            if (checkerKinds.isEmpty()) {
                emptyList<KaDiagnosticWithPsi<*>>().iterator()
            } else {
                compute().iterator()
            }
        }

        private fun compute(): Sequence<KaDiagnosticWithPsi<*>> = element.withPsiValidityAssertion {
            val filter = checkerKinds.asLLFilter()

            val diagnostics = if (isRecursive) {
                element.diagnostics(resolutionFacade, filter)
            } else {
                element.getDiagnostics(resolutionFacade, filter).asSequence()
            }

            diagnostics
                .filter { includeSuppressed || !it.isSuppressed }
                .map { it.asKaDiagnostic() }
        }

        private fun Set<KaDiagnosticCheckerKind>.asLLFilter() = DiagnosticCheckerFilter(
            runDefaultCheckers = KaDiagnosticCheckerKind.COMMON in this,
            runExtraCheckers = KaDiagnosticCheckerKind.EXTENDED in this,
            runExperimentalCheckers = KaDiagnosticCheckerKind.EXPERIMENTAL in this,
        )
    }

    private companion object {
        private val DEFAULT_CHECKER_KINDS = setOf(KaDiagnosticCheckerKind.COMMON)
    }
}
