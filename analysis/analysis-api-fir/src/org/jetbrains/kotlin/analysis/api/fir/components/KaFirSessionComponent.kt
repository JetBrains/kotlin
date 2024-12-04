/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.diagnostics.KT_DIAGNOSTIC_CONVERTER
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
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

internal interface KaFirSessionComponent {
    val analysisSession: KaFirSession

    val project: Project get() = analysisSession.project
    val rootModuleSession: FirSession get() = analysisSession.firResolveSession.useSiteFirSession
    val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
    val firSymbolBuilder get() = analysisSession.firSymbolBuilder
    val firResolveSession get() = analysisSession.firResolveSession

    fun ConeKotlinType.asKtType() = analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)

    fun KtPsiDiagnostic.asKtDiagnostic(): KaDiagnosticWithPsi<*> =
        KT_DIAGNOSTIC_CONVERTER.convert(analysisSession, this as KtDiagnostic)

    fun ConeDiagnostic.asKtDiagnostic(
        source: KtSourceElement,
        callOrAssignmentSource: KtSourceElement?,
    ): KaDiagnosticWithPsi<*>? {
        val firDiagnostic = toFirDiagnostics(analysisSession.firSession, source, callOrAssignmentSource).firstOrNull() ?: return null
        check(firDiagnostic is KtPsiDiagnostic)
        return firDiagnostic.asKtDiagnostic()
    }

    val KaType.coneType: ConeKotlinType
        get() {
            require(this is KaFirType)
            return coneType
        }

    val KaTypeProjection.coneTypeProjection: ConeTypeProjection
        get() = when (this) {
            is KaStarTypeProjection -> ConeStarProjection
            is KaTypeArgumentWithVariance -> {
                typeContext.createTypeArgument(type.coneType, variance.convertVariance()) as ConeTypeProjection
            }
        }

    fun createTypeCheckerContext(errorTypePolicy: KaSubtypingErrorTypePolicy): TypeCheckerState {
        // TODO use correct session here,
        return analysisSession.firResolveSession.useSiteFirSession.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT,
            stubTypesEqualToAnything = true,
        )
    }

    fun FirQualifiedAccessExpression.createSubstitutorFromTypeArguments(discardErrorTypes: Boolean = false): KaSubstitutor? {
        return createConeSubstitutorFromTypeArguments(rootModuleSession, discardErrorTypes)?.toKtSubstitutor()
    }

    fun FirQualifiedAccessExpression.createSubstitutorFromTypeArguments(
        callableSymbol: FirCallableSymbol<*>,
        discardErrorTypes: Boolean = false
    ): KaSubstitutor {
        return createConeSubstitutorFromTypeArguments(callableSymbol, rootModuleSession, discardErrorTypes).toKtSubstitutor()
    }

    fun ConeSubstitutor.toKtSubstitutor(): KaSubstitutor {
        return analysisSession.firSymbolBuilder.typeBuilder.buildSubstitutor(this)
    }
}
