/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.diagnostics

import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticCheckerKind
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSessionBoundOwner
import org.jetbrains.kotlin.analysis.api.fir.asKaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.components.resolutionFacade
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.DiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.diagnostics
import org.jetbrains.kotlin.psi.KtElement

/**
 * A [KaDiagnostics] query. The query is immutable: every modifier returns a new instance, so it can be safely reused and adjusted.
 */
internal class KaFirDiagnostics(
    private val element: KtElement,
    private val directOnly: Boolean,
    private val checkerKinds: Set<KaDiagnosticCheckerKind>,
    private val includingSuppressed: Boolean,
    override val analysisSession: KaFirSession,
) : KaDiagnostics, KaFirSessionBoundOwner {
    override fun withCheckers(kinds: Set<KaDiagnosticCheckerKind>): KaDiagnostics = entryPointImplementation {
        if (kinds == checkerKinds) return this

        KaFirDiagnostics(
            element = element,
            directOnly = directOnly,
            checkerKinds = kinds.toSet(),
            includingSuppressed = includingSuppressed,
            analysisSession = analysisSession,
        )
    }

    override fun withCheckers(vararg kinds: KaDiagnosticCheckerKind): KaDiagnostics = withCheckers(kinds.toSet())

    override fun includingSuppressed(include: Boolean): KaDiagnostics = entryPointImplementation {
        if (includingSuppressed == include) return this

        KaFirDiagnostics(
            element = element,
            directOnly = directOnly,
            checkerKinds = checkerKinds,
            includingSuppressed = include,
            analysisSession = analysisSession,
        )
    }

    override fun directOnly(direct: Boolean): KaDiagnostics = entryPointImplementation {
        if (directOnly == direct) return this

        KaFirDiagnostics(
            element = element,
            directOnly = direct,
            checkerKinds = checkerKinds,
            includingSuppressed = includingSuppressed,
            analysisSession = analysisSession,
        )
    }

    override fun iterator(): Iterator<KaDiagnosticWithPsi<*>> = entryPointImplementation {
        // There is nothing to compute if no checkers were requested
        if (checkerKinds.isEmpty()) {
            emptyList<KaDiagnosticWithPsi<*>>().iterator()
        } else {
            compute().iterator()
        }
    }

    private fun compute(): Sequence<KaDiagnosticWithPsi<*>> = entryPointImplementation(element) {
        val filter = checkerKinds.asLLFilter()

        element.diagnostics(resolutionFacade = resolutionFacade, filter = filter, isRecursive = !directOnly)
            .filter { includingSuppressed || !it.isSuppressed }
            .map { it.asKaDiagnostic() }
    }

    private fun Set<KaDiagnosticCheckerKind>.asLLFilter() = DiagnosticCheckerFilter(
        runDefaultCheckers = KaDiagnosticCheckerKind.COMMON in this,
        runExtraCheckers = KaDiagnosticCheckerKind.EXTENDED in this,
        runExperimentalCheckers = KaDiagnosticCheckerKind.EXPERIMENTAL in this,
    )
}
