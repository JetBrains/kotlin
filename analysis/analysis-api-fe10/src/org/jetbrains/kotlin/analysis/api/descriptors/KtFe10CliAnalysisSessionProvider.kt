/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors

import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityTokenFactory
import org.jetbrains.kotlin.psi.KtElement

@InvalidWayOfUsingAnalysisSession
class KtFe10AnalysisSessionProvider : KtAnalysisSessionProvider() {
    @InvalidWayOfUsingAnalysisSession
    override fun getAnalysisSession(contextElement: KtElement, factory: ValidityTokenFactory): KtAnalysisSession {
        return KtFe10AnalysisSession(contextElement, factory.create(contextElement.project))
    }

    @InvalidWayOfUsingAnalysisSession
    override fun getAnalysisSessionBySymbol(contextSymbol: KtSymbol): KtAnalysisSession {
        if (contextSymbol is KtFe10Symbol) {
            return contextSymbol.analysisSession
        } else {
            val contextElement = contextSymbol.psi
            if (contextElement != null) {
                return KtFe10AnalysisSession(contextElement, contextSymbol.token)
            }
        }

        throw IllegalArgumentException("Unsupported symbol kind: $contextSymbol")
    }

    override fun clearCaches() {}
}