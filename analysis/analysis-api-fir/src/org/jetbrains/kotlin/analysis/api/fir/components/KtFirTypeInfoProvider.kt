/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtTypeInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.resolve.FirSamResolverImpl
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.functionClassKind
import org.jetbrains.kotlin.fir.types.typeApproximator

internal class KtFirTypeInfoProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken
) : KtTypeInfoProvider(), KtFirAnalysisSessionComponent {

    override fun isFunctionalInterfaceType(type: KtType): Boolean {
        val coneType = (type as KtFirType).coneType
        val firSession = analysisSession.useSiteSession
        val samResolver = FirSamResolverImpl(
            firSession,
            analysisSession.getScopeSessionFor(firSession),
        )
        return samResolver.getSamInfoForPossibleSamType(coneType) != null
    }

    override fun getFunctionClassKind(type: KtType): FunctionClassKind? {
        val coneType = (type as KtFirType).coneType
        return coneType.functionClassKind(analysisSession.useSiteSession)
    }

    override fun canBeNull(type: KtType): Boolean = (type as KtFirType).coneType.canBeNull

    override fun isDenotable(type: KtType): Boolean {
        val coneType = (type as KtFirType).coneType
        return analysisSession.useSiteSession.typeApproximator.approximateToSuperType(
            coneType,
            PublicTypeApproximator.PublicApproximatorConfiguration(false)
        ) == null
    }
}
