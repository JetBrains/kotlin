/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.types.KaFirType
import org.jetbrains.kotlin.analysis.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.resolve.FirSamResolver
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

internal class KaFirTypeInformationProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaTypeInformationProvider, KaFirSessionComponent {
    override val KaType.isFunctionalInterface: Boolean
        get() = withValidityAssertion {
            val coneType = (this as KaFirType).coneType
            val firSession = analysisSession.firSession
            val samResolver = FirSamResolver(
                firSession,
                analysisSession.getScopeSessionFor(firSession),
            )
            return samResolver.isSamType(coneType)
        }

    override val KaType.functionTypeKind: FunctionTypeKind?
        get() = withValidityAssertion {
            (this as KaFirType).coneType.functionTypeKind(analysisSession.firSession)
        }

    override val KaType.isNullable: Boolean
        get() = withValidityAssertion {
            (this as KaFirType).coneType.canBeNull(analysisSession.firSession)
        }

    override val KaType.isDenotable: Boolean
        get() = withValidityAssertion {
            val coneType = (this as KaFirType).coneType
            return analysisSession.firSession.typeApproximator.approximateToSuperType(
                coneType,
                PublicTypeApproximator.PublicApproximatorConfiguration(false)
            ) == null
        }

    override val KaType.isArrayOrPrimitiveArray: Boolean
        get() = withValidityAssertion {
            require(this is KaFirType)
            return coneType.isArrayOrPrimitiveArray
        }

    override val KaType.isNestedArray: Boolean
        get() = withValidityAssertion {
            if (!isArrayOrPrimitiveArray) return false
            require(this is KaFirType)
            return coneType.arrayElementType()?.isArrayOrPrimitiveArray == true
        }

    override val KaType.fullyExpandedType: KaType
        get() = withValidityAssertion {
            coneType.fullyExpandedType(analysisSession.firSession).asKtType()
        }
}
