/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticCheckerKind
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostics
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsDiagnosticProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Routes the legacy [KaDiagnosticProvider] surface straight to the [KaInternalsDiagnosticProvider] proxy, expressing the legacy
 * [KaDiagnosticCheckerFilter] as a [KaDiagnostics] query. The public `context(session: KaSession)` diagnostic endpoints were promoted in
 * place (in the `components` package) and reach the proxy through this bridge, so the bridge must forward directly to the proxy to avoid
 * recursing back into the endpoints.
 */
@OptIn(KaExperimentalApi::class)
internal class KaDiagnosticProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaDiagnosticProvider {
    private val proxy: KaInternalsDiagnosticProvider
        get() = analysisSession.diagnosticProvider

    @Suppress("OVERRIDE_DEPRECATION")
    override fun KtElement.diagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        query(isRecursive = false, filter).toList()

    override fun KtElement.directDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        query(isRecursive = false, filter).toList()

    override fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        query(isRecursive = true, filter).toList()

    override fun KtFile.diagnostics(filter: KaDiagnosticCheckerFilter): Sequence<KaDiagnosticWithPsi<*>> =
        query(isRecursive = true, filter)

    override fun KtFile.diagnosticsIgnoringSuppression(filter: KaDiagnosticCheckerFilter): Sequence<KaDiagnosticWithPsi<*>> =
        query(isRecursive = true, filter).includingSuppressed()

    private fun KtElement.query(isRecursive: Boolean, filter: KaDiagnosticCheckerFilter): KaDiagnostics =
        proxy.diagnostics(this, isRecursive).withCheckers(filter.asCheckerKinds())

    private fun KaDiagnosticCheckerFilter.asCheckerKinds(): Set<KaDiagnosticCheckerKind> = when (this) {
        KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS -> setOf(KaDiagnosticCheckerKind.COMMON)
        KaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS -> setOf(KaDiagnosticCheckerKind.EXTENDED)
        KaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS -> setOf(KaDiagnosticCheckerKind.EXPERIMENTAL)
        KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS -> setOf(
            KaDiagnosticCheckerKind.COMMON,
            KaDiagnosticCheckerKind.EXTENDED,
        )
    }
}
