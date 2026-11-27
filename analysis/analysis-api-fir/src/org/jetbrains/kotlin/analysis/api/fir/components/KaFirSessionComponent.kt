/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSessionBoundOwner
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLResolutionFacade
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.typeContext

internal abstract class KaFirSessionComponent : KaFirSessionBoundOwner {
    abstract val analysisSessionProvider: () -> KaFirSession

    final override val analysisSession: KaFirSession
        get() = analysisSessionProvider()
}

context(analysisSession: KaFirSession)
internal val project: Project
    get() = analysisSession.project

context(analysisSession: KaFirSession)
internal val rootModuleSession: FirSession
    get() = analysisSession.resolutionFacade.useSiteFirSession

context(analysisSession: KaFirSession)
internal val typeContext: ConeInferenceContext
    get() = rootModuleSession.typeContext

context(analysisSession: KaFirSession)
internal val firSymbolBuilder: KaSymbolByFirBuilder
    get() = analysisSession.firSymbolBuilder

context(analysisSession: KaFirSession)
internal val resolutionFacade: LLResolutionFacade
    get() = analysisSession.resolutionFacade
