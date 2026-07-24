/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseTypeInformationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

internal class KaFirTypeInformationProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseTypeInformationProvider<KaFirSession>(), KaFirSessionComponent {
    override fun isFunctionalInterface(type: KaType): Boolean = type.withValidityAssertion {
        val coneType = (type as KaFirType).coneType
        val firSession = analysisSession.firSession
        val samResolver = FirSamResolver(
            firSession,
            analysisSession.getScopeSessionFor(firSession),
        )
        return samResolver.isSamType(coneType)
    }

    override fun computeFunctionTypeKind(type: KaType): FunctionTypeKind? {
        return (type as KaFirType).coneType.functionTypeKind(analysisSession.firSession)
    }

    override fun isNullable(type: KaType): Boolean = type.withValidityAssertion {
        (type as KaFirType).coneType.canBeNull(analysisSession.firSession)
    }

    override fun isMarkedNullable(type: KaType): Boolean = type.withValidityAssertion {
        (type as KaFirType).coneType.isMarkedNullable
    }

    override fun hasFlexibleNullability(type: KaType): Boolean = type.withValidityAssertion {
        val coneType = (type as KaFirType).coneType
        coneType.hasFlexibleMarkedNullability || coneType is ConeErrorType && coneType.nullable == null
    }

    override fun isDenotable(type: KaType): Boolean = type.withValidityAssertion {
        with(analysisSession) {
            type.approximateToDenotableSupertype(allowLocalDenotableTypes = true) == null
        }
    }

    override fun isArrayOrPrimitiveArray(type: KaType): Boolean = type.withValidityAssertion {
        require(type is KaFirType)
        return type.coneType.isArrayOrPrimitiveArray
    }

    override fun isNestedArray(type: KaType): Boolean = type.withValidityAssertion {
        if (!isArrayOrPrimitiveArray(type)) return false
        require(type is KaFirType)
        return type.coneType.arrayElementType()?.isArrayOrPrimitiveArray == true
    }

    override fun fullyExpandedType(type: KaType): KaType = type.withValidityAssertion {
        (type as KaFirType).coneType.fullyExpandedType(analysisSession.firSession).asKaType()
    }
}
