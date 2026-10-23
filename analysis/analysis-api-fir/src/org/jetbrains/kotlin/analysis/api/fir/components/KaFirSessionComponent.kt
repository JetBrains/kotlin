/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.asKaDiagnostic
import org.jetbrains.kotlin.analysis.api.fir.utils.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaSubtypingErrorTypePolicy
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLDiagnostic
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.types.TypeCheckerState

internal abstract class KaFirSessionComponent : KaSessionComponent {
    abstract val analysisSessionProvider: () -> KaFirSession

    val analysisSession: KaFirSession
        get() = analysisSessionProvider()

    final override val token: KaLifetimeToken
        get() = analysisSession.token
}

context(sessionComponent: KaFirSessionComponent)
internal val analysisSession: KaFirSession
    get() = sessionComponent.analysisSession

context(component: KaFirSessionComponent)
internal val project: Project
    get() = component.analysisSession.project

context(component: KaFirSessionComponent)
internal val rootModuleSession: FirSession
    get() = component.analysisSession.resolutionFacade.useSiteFirSession

context(component: KaFirSessionComponent)
internal val typeContext: ConeInferenceContext
    get() = rootModuleSession.typeContext

context(component: KaFirSessionComponent)
internal val firSymbolBuilder: KaSymbolByFirBuilder
    get() = component.analysisSession.firSymbolBuilder

context(component: KaFirSessionComponent)
internal val resolutionFacade: LLResolutionFacade
    get() = component.analysisSession.resolutionFacade

context(component: KaFirSessionComponent)
internal fun ConeKotlinType.asKaType(): KaType = asKaType(component.analysisSession)

context(component: KaFirSessionComponent)
internal fun KtDiagnosticWithSource.asKaDiagnostic(): KaDiagnosticWithPsi<*> = asKaDiagnostic(component.analysisSession)

context(component: KaFirSessionComponent)
internal fun LLDiagnostic.asKaDiagnostic(): KaDiagnosticWithPsi<*> = asKaDiagnostic(component.analysisSession)

context(component: KaFirSessionComponent)
internal fun ConeDiagnostic.asKaDiagnostic(
    source: KtSourceElement,
): KaDiagnosticWithPsi<*>? = asKaDiagnostic(source = source, component.analysisSession)

context(component: KaFirSessionComponent)
internal val KaTypeProjection.coneTypeProjection: ConeTypeProjection
    get() = coneTypeProjection(component.analysisSession)

context(component: KaFirSessionComponent)
internal fun createTypeCheckerContext(errorTypePolicy: KaSubtypingErrorTypePolicy): TypeCheckerState =
    createTypeCheckerContext(errorTypePolicy, component.analysisSession)

context(component: KaFirSessionComponent)
internal fun ConeSubstitutor.toKaSubstitutor(): KaSubstitutor = toKaSubstitutor(component.analysisSession)
