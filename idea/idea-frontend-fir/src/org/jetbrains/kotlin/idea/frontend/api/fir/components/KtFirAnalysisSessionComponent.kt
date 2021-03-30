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
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerContext
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.diagnostics.KT_DIAGNOSTIC_CONVERTER

internal interface KtFirAnalysisSessionComponent {
    val analysisSession: KtFirAnalysisSession

    val rootModuleSession: FirSession get() = analysisSession.firResolveState.rootModuleSession
    val firSymbolBuilder get() = analysisSession.firSymbolBuilder
    val firResolveState get() = analysisSession.firResolveState

    fun ConeKotlinType.asKtType() = analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)

    fun FirPsiDiagnostic<*>.asKtDiagnostic(): KtDiagnosticWithPsi<*> =
        KT_DIAGNOSTIC_CONVERTER.convert(analysisSession, this as FirDiagnostic<*>)

    fun ConeDiagnostic.asKtDiagnostic(source: FirSourceElement, qualifiedAccessSource: FirSourceElement?): KtDiagnosticWithPsi<*>? {
        val firDiagnostic = toFirDiagnostics(source, qualifiedAccessSource).firstOrNull() ?: return null
        check(firDiagnostic is FirPsiDiagnostic<*>)
        return firDiagnostic.asKtDiagnostic()
    }

    fun createTypeCheckerContext() = ConeTypeCheckerContext(
        isErrorTypeEqualsToAnything = true,
        isStubTypeEqualsToAnything = true,
        analysisSession.firResolveState.rootModuleSession.typeContext //TODO use correct session here
    )
}
