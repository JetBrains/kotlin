/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaTypeInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

internal class KaFirTypeInfoProvider(
    override val analysisSession: KaFirSession,
    override val token: KaLifetimeToken
) : KaTypeInfoProvider(), KaFirSessionComponent {

    override fun isFunctionalInterfaceType(type: KaType): Boolean {
        val coneType = (type as KaFirType).coneType
        val firSession = analysisSession.useSiteSession
        val samResolver = FirSamResolver(
            firSession,
            analysisSession.getScopeSessionFor(firSession),
        )
        return samResolver.isSamType(coneType)
    }

    override fun getFunctionClassKind(type: KaType): FunctionTypeKind? {
        return (type as KaFirType).coneType.functionTypeKind(analysisSession.useSiteSession)
    }

    override fun canBeNull(type: KaType): Boolean = (type as KaFirType).coneType.canBeNull(analysisSession.useSiteSession)

    override fun isDenotable(type: KaType): Boolean {
        val coneType = (type as KaFirType).coneType
        return analysisSession.useSiteSession.typeApproximator.approximateToSuperType(
            coneType,
            PublicTypeApproximator.PublicApproximatorConfiguration(false)
        ) == null
    }

    override fun isArrayOrPrimitiveArray(type: KaType): Boolean {
        require(type is KaFirType)
        return type.coneType.isArrayOrPrimitiveArray
    }

    override fun isNestedArray(type: KaType): Boolean {
        if (!isArrayOrPrimitiveArray(type)) return false
        require(type is KaFirType)
        return type.coneType.arrayElementType()?.isArrayOrPrimitiveArray == true
    }

    override fun fullyExpandedType(type: KaType): KaType = type.coneType.fullyExpandedType(analysisSession.useSiteSession).asKtType()
}
