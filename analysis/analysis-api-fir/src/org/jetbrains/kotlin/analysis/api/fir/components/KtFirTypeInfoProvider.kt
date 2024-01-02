/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtTypeInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

internal class KtFirTypeInfoProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken
) : KtTypeInfoProvider(), KtFirAnalysisSessionComponent {

    override fun isFunctionalInterfaceType(type: KtType): Boolean {
        val coneType = (type as KtFirType).coneType
        val firSession = analysisSession.useSiteSession
        val samResolver = FirSamResolver(
            firSession,
            analysisSession.getScopeSessionFor(firSession),
        )
        return samResolver.isSamType(coneType)
    }

    override fun getFunctionClassKind(type: KtType): FunctionTypeKind? {
        return (type as KtFirType).coneType.functionTypeKind(analysisSession.useSiteSession)
    }

    override fun canBeNull(type: KtType): Boolean = (type as KtFirType).coneType.canBeNull(analysisSession.useSiteSession)

    override fun isDenotable(type: KtType): Boolean {
        val coneType = (type as KtFirType).coneType
        return analysisSession.useSiteSession.typeApproximator.approximateToSuperType(
            coneType,
            PublicTypeApproximator.PublicApproximatorConfiguration(false)
        ) == null
    }

    override fun isArrayOrPrimitiveArray(type: KtType): Boolean {
        require(type is KtFirType)
        return type.coneType.isArrayOrPrimitiveArray
    }

    override fun isNestedArray(type: KtType): Boolean {
        if (!isArrayOrPrimitiveArray(type)) return false
        require(type is KtFirType)
        return type.coneType.arrayElementType()?.isArrayOrPrimitiveArray == true
    }

    override fun fullyExpandedType(type: KtType): KtType = type.coneType.fullyExpandedType(analysisSession.useSiteSession).asKtType()
}
