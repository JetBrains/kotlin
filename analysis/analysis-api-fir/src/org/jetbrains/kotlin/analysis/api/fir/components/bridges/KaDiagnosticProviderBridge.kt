/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticProvider
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsDiagnosticProvider
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

/**
 * Routes the legacy [KaDiagnosticProvider] surface straight to the [KaInternalsDiagnosticProvider] proxy. The public
 * `context(session: KaSession)` diagnostic endpoints were promoted in place (in the `components` package) and reach the
 * proxy through this bridge, so the bridge must forward directly to the proxy to avoid recursing back into the endpoints.
 */
internal class KaDiagnosticProviderBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaDiagnosticProvider {
    private val proxy: KaInternalsDiagnosticProvider
        get() = analysisSession.diagnosticProvider

    @Suppress("OVERRIDE_DEPRECATION")
    override fun KtElement.diagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        proxy.directDiagnostics(this, filter)

    override fun KtElement.directDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        proxy.directDiagnostics(this, filter)

    override fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        proxy.collectDiagnostics(this, filter)

    override fun KtFile.diagnostics(filter: KaDiagnosticCheckerFilter): Sequence<KaDiagnosticWithPsi<*>> =
        proxy.diagnostics(this, filter)
}
