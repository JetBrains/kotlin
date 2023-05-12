/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.components.KtScopeSubstitution
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.scopes.KtFirDelegatingNamesAwareScope
import org.jetbrains.kotlin.analysis.api.fir.scopes.KtFirDelegatingTypeScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KtCompositeScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KtCompositeTypeScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtTypeScope
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

internal class KtFirScopeSubstitution(
    override val analysisSession: KtFirAnalysisSession,
) : KtScopeSubstitution(), KtFirAnalysisSessionComponent {

    @OptIn(KtAnalysisApiInternals::class)
    override fun getDeclarationScope(scope: KtTypeScope): KtScope {
        return when (scope) {
            is KtFirDelegatingTypeScope -> KtFirDelegatingNamesAwareScope(scope.firScope, analysisSession.firSymbolBuilder)
            is KtCompositeTypeScope -> KtCompositeScope.create(scope.subScopes.map(::getDeclarationScope), token)
            else -> unexpectedElementError<KtTypeScope>(scope)
        }
    }
}