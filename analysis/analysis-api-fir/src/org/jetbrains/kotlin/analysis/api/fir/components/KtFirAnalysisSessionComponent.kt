/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KtTypeProjection
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.diagnostics.KT_DIAGNOSTIC_CONVERTER
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.createConeSubstitutorFromTypeArguments
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.convertVariance

internal interface KtFirAnalysisSessionComponent {
    val analysisSession: KtFirAnalysisSession

    val project: Project get() = analysisSession.project
    val rootModuleSession: FirSession get() = analysisSession.firResolveSession.useSiteFirSession
    val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
    val firSymbolBuilder get() = analysisSession.firSymbolBuilder
    val firResolveSession get() = analysisSession.firResolveSession

    fun ConeKotlinType.asKtType() = analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)

    fun KtPsiDiagnostic.asKtDiagnostic(): KtDiagnosticWithPsi<*> =
        KT_DIAGNOSTIC_CONVERTER.convert(analysisSession, this as KtDiagnostic)

    fun ConeDiagnostic.asKtDiagnostic(
        source: KtSourceElement,
        callOrAssignmentSource: KtSourceElement?,
    ): KtDiagnosticWithPsi<*>? {
        val firDiagnostic = toFirDiagnostics(analysisSession.useSiteSession, source, callOrAssignmentSource).firstOrNull() ?: return null
        check(firDiagnostic is KtPsiDiagnostic)
        return firDiagnostic.asKtDiagnostic()
    }

    val KtType.coneType: ConeKotlinType
        get() {
            require(this is KtFirType)
            return coneType
        }

    val KtTypeProjection.coneTypeProjection: ConeTypeProjection
        get() = when (this) {
            is KtStarTypeProjection -> ConeStarProjection
            is KtTypeArgumentWithVariance -> {
                typeContext.createTypeArgument(type.coneType, variance.convertVariance()) as ConeTypeProjection
            }
        }

    fun createTypeCheckerContext(): TypeCheckerState {
        // TODO use correct session here,
        return analysisSession.firResolveSession.useSiteFirSession.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = true
        )
    }

    fun FirQualifiedAccessExpression.createSubstitutorFromTypeArguments(discardErrorTypes: Boolean = false): KtSubstitutor? {
        return createConeSubstitutorFromTypeArguments(rootModuleSession, discardErrorTypes)?.toKtSubstitutor()
    }

    fun FirQualifiedAccessExpression.createSubstitutorFromTypeArguments(
        callableSymbol: FirCallableSymbol<*>,
        discardErrorTypes: Boolean = false
    ): KtSubstitutor {
        return createConeSubstitutorFromTypeArguments(callableSymbol, rootModuleSession, discardErrorTypes).toKtSubstitutor()
    }

    fun ConeSubstitutor.toKtSubstitutor(): KtSubstitutor {
        return analysisSession.firSymbolBuilder.typeBuilder.buildSubstitutor(this)
    }
}
