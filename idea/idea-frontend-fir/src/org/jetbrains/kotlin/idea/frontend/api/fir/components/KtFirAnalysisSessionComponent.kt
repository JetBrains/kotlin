/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.idea.frontend.api.KtStarProjectionTypeArgument
import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgument
import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerState
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KT_DIAGNOSTIC_CONVERTER
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.types.model.convertVariance

internal interface KtFirAnalysisSessionComponent {
    val analysisSession: KtFirAnalysisSession

    val rootModuleSession: FirSession get() = analysisSession.firResolveState.rootModuleSession
    val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
    val firSymbolBuilder get() = analysisSession.firSymbolBuilder
    val firResolveState get() = analysisSession.firResolveState

    fun ConeKotlinType.asKtType() = analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)

    fun FirPsiDiagnostic.asKtDiagnostic(): KtDiagnosticWithPsi<*> =
        KT_DIAGNOSTIC_CONVERTER.convert(analysisSession, this as FirDiagnostic)

    fun ConeDiagnostic.asKtDiagnostic(
        source: FirSourceElement,
        qualifiedAccessSource: FirSourceElement?,
        diagnosticCache: MutableList<FirDiagnostic>
    ): KtDiagnosticWithPsi<*>? {
        val firDiagnostic = toFirDiagnostics(source, qualifiedAccessSource).firstOrNull() ?: return null
        diagnosticCache += firDiagnostic
        check(firDiagnostic is FirPsiDiagnostic)
        return firDiagnostic.asKtDiagnostic()
    }

    val KtType.coneType: ConeKotlinType
        get() {
            require(this is KtFirType)
            return coneType
        }

    val KtTypeArgument.coneTypeProjection: ConeTypeProjection
        get() = when (this) {
            is KtStarProjectionTypeArgument -> ConeStarProjection
            is KtTypeArgumentWithVariance -> {
                typeContext.createTypeArgument(type.coneType, variance.convertVariance()) as ConeTypeProjection
            }
        }

    fun createTypeCheckerContext() = ConeTypeCheckerState(
        isErrorTypeEqualsToAnything = true,
        isStubTypeEqualsToAnything = true,
        analysisSession.firResolveState.rootModuleSession.typeContext //TODO use correct session here
    )
}
