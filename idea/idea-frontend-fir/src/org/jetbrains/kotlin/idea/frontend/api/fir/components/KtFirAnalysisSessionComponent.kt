/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeCheckerContext
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.idea.frontend.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession

internal interface KtFirAnalysisSessionComponent {
    val analysisSession: KtFirAnalysisSession

    val rootModuleSession: FirSession get() = analysisSession.firResolveState.rootModuleSession

    val firSymbolBuilder get() = analysisSession.firSymbolBuilder
    val firResolveState get() = analysisSession.firResolveState
    fun ConeKotlinType.asKtType() = analysisSession.firSymbolBuilder.buildKtType(this)

    fun FirPsiDiagnostic<*>.asKtDiagnostic(): KtDiagnosticWithPsi<*> =
        (analysisSession.diagnosticProvider as KtFirDiagnosticProvider).firDiagnosticAsKtDiagnostic(this)

    fun ConeDiagnostic.asKtDiagnostic(source: FirSourceElement): KtDiagnostic? =
        (analysisSession.diagnosticProvider as KtFirDiagnosticProvider).coneDiagnosticAsKtDiagnostic(this, source)

    fun createTypeCheckerContext() = ConeTypeCheckerContext(
        isErrorTypeEqualsToAnything = true,
        isStubTypeEqualsToAnything = true,
        analysisSession.firResolveState.rootModuleSession //TODO use correct session here
    )
}