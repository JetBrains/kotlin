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
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.asKaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.utils.*
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.types.TypeCheckerState

internal interface KaFirSessionComponent : KaSessionComponent {
    val analysisSession: KaFirSession

    val project: Project get() = analysisSession.project
    val rootModuleSession: FirSession get() = analysisSession.resolutionFacade.useSiteFirSession
    val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
    val firSymbolBuilder: KaSymbolByFirBuilder get() = analysisSession.firSymbolBuilder
    val resolutionFacade: LLResolutionFacade get() = analysisSession.resolutionFacade

    fun ConeKotlinType.asKaType(): KaType = asKaType(analysisSession)

    fun KtPsiDiagnostic.asKaDiagnostic(): KaDiagnosticWithPsi<*> = asKaDiagnostic(analysisSession)

    fun ConeDiagnostic.asKaDiagnostic(
        source: KtSourceElement,
        callOrAssignmentSource: KtSourceElement?,
    ): KaDiagnosticWithPsi<*>? = asKaDiagnostic(source, callOrAssignmentSource, analysisSession)

    val KaType.coneType: ConeKotlinType
        get() {
            require(this is KaFirType)
            return coneType
        }

    val KaTypeProjection.coneTypeProjection: ConeTypeProjection
        get() = coneTypeProjection(analysisSession)

    fun createTypeCheckerContext(errorTypePolicy: KaSubtypingErrorTypePolicy): TypeCheckerState =
        createTypeCheckerContext(errorTypePolicy, analysisSession)

    fun ConeSubstitutor.toKaSubstitutor(): KaSubstitutor = toKaSubstitutor(analysisSession)
}
