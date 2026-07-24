/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaUseSiteVisibilityChecker
import org.jetbrains.kotlin.analysis.api.components.KaVisibilityChecker
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.analysis.api.visibility.createUseSiteVisibilityChecker as createUseSiteVisibilityCheckerEndpoint
import org.jetbrains.kotlin.analysis.api.visibility.isPublicApi as isPublicApiEndpoint
import org.jetbrains.kotlin.analysis.api.visibility.isVisibleInClass as isVisibleInClassEndpoint

/**
 * Routes the legacy [KaVisibilityChecker] surface through the new public `context(session: KaSession)` visibility endpoints, which in turn
 * reach the [org.jetbrains.kotlin.analysis.api.internals.KaInternalsVisibilityChecker] proxy.
 *
 * The deprecated [KaVisibilityChecker.isVisible] member is intentionally not overridden: it keeps its interface default body, which composes
 * [createUseSiteVisibilityChecker] (and therefore routes through the new endpoint as well).
 */
internal class KaVisibilityCheckerBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaVisibilityChecker {
    override fun createUseSiteVisibilityChecker(
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression?,
        position: PsiElement,
    ): KaUseSiteVisibilityChecker =
        context(analysisSession) {
            // The endpoint now returns the new visibility.KaUseSiteVisibilityChecker. Its only implementation also implements the legacy
            // components.KaUseSiteVisibilityChecker, so narrowing the result back to the legacy surface is safe.
            createUseSiteVisibilityCheckerEndpoint(useSiteFile, receiverExpression, position) as KaUseSiteVisibilityChecker
        }

    override fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassSymbol): Boolean =
        context(analysisSession) { isVisibleInClassEndpoint(classSymbol) }

    override fun isPublicApi(symbol: KaDeclarationSymbol): Boolean =
        context(analysisSession) { symbol.isPublicApiEndpoint }
}
