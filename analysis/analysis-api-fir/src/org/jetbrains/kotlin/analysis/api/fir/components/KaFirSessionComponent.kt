/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.components.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.diagnostics.KT_DIAGNOSTIC_CONVERTER
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.toKtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.convertVariance

internal interface KaFirSessionComponent : KaSessionComponent {
    val analysisSession: KaFirSession

    val project: Project get() = analysisSession.project
    val rootModuleSession: FirSession get() = analysisSession.resolutionFacade.useSiteFirSession
    val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
    val firSymbolBuilder get() = analysisSession.firSymbolBuilder
    val resolutionFacade get() = analysisSession.resolutionFacade

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
        return analysisSession.resolutionFacade.useSiteFirSession.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = errorTypePolicy == KaSubtypingErrorTypePolicy.LENIENT,
            stubTypesEqualToAnything = true,
        )
    }

    fun ConeSubstitutor.toKtSubstitutor(): KaSubstitutor = toKtSubstitutor(analysisSession)
}
