/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaCompletionCandidateChecker
import org.jetbrains.kotlin.analysis.api.components.KaCompletionExtensionCandidateChecker
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsCompletionCandidateChecker
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

/**
 * Routes the legacy [KaCompletionCandidateChecker] surface straight to the
 * [KaInternalsCompletionCandidateChecker] proxy. This component has no public
 * `context(session: KaSession)` endpoint, so the single member is forwarded directly to the proxy.
 */
@KaImplementationDetail
internal class KaCompletionCandidateCheckerBridge(
    override val analysisSessionProvider: () -> KaFirSession,
) : KaBaseSessionComponent<KaFirSession>(), KaCompletionCandidateChecker {
    private val proxy: KaInternalsCompletionCandidateChecker
        get() = analysisSession.completionCandidateChecker

    @KaIdeApi
    override fun createExtensionCandidateChecker(
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?,
    ): KaCompletionExtensionCandidateChecker =
        proxy.createExtensionCandidateChecker(originalFile, nameExpression, explicitReceiver)
}
