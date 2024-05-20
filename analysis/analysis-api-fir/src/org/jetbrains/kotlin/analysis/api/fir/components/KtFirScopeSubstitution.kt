/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.components.KaScopeSubstitution
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.scopes.KaFirDelegatingNamesAwareScope
import org.jetbrains.kotlin.analysis.api.fir.scopes.KaFirDelegatingTypeScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaCompositeScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaCompositeTypeScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.utils.errors.unexpectedElementError

internal class KaFirScopeSubstitution(
    override val analysisSession: KaFirSession,
) : KaScopeSubstitution(), KaFirSessionComponent {

    @OptIn(KaAnalysisApiInternals::class)
    override fun getDeclarationScope(scope: KaTypeScope): KaScope {
        return when (scope) {
            is KaFirDelegatingTypeScope -> KaFirDelegatingNamesAwareScope(scope.firScope, analysisSession.firSymbolBuilder)
            is KaCompositeTypeScope -> KaCompositeScope.create(scope.subScopes.map(::getDeclarationScope), token)
            else -> unexpectedElementError<KaTypeScope>(scope)
        }
    }
}