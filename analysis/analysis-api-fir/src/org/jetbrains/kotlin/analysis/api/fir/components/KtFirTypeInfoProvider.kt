/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.fir.resolve.FirSamResolverImpl
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.analysis.api.components.KtTypeInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.types.functionClassKind

internal class KtFirTypeInfoProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtTypeInfoProvider(), KtFirAnalysisSessionComponent {

    override fun isFunctionalInterfaceType(type: KtType): Boolean {
        val coneType = (type as KtFirType).coneType
        val samResolver = FirSamResolverImpl(analysisSession.rootModuleSession, ScopeSession())
        return samResolver.getFunctionTypeForPossibleSamType(coneType) != null
    }

    override fun getFunctionClassKind(type: KtType): FunctionClassKind? {
        val coneType = (type as KtFirType).coneType
        return coneType.functionClassKind(analysisSession.rootModuleSession)
    }

    override fun canBeNull(type: KtType): Boolean = (type as KtFirType).coneType.canBeNull
}
