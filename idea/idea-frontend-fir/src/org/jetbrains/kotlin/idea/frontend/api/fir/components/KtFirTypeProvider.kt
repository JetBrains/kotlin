/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.resolve.inference.inferenceComponents
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtBuiltinTypes
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeProvider
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.types.KtFirType
import org.jetbrains.kotlin.idea.frontend.api.fir.types.PublicTypeApproximator
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration

internal class KtFirTypeProvider(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtTypeProvider(), KtFirAnalysisSessionComponent {
    override val builtinTypes: KtBuiltinTypes =
        KtFirBuiltInTypes(analysisSession.firResolveState.rootModuleSession.builtinTypes, analysisSession.firSymbolBuilder, token)

    override fun approximateToSuperPublicDenotableType(type: KtType): KtType? {
        require(type is KtFirType)
        val coneType = type.coneType
        val approximatedConeType = PublicTypeApproximator.approximateTypeToPublicDenotable(
            coneType,
            rootModuleSession
        )

        return approximatedConeType?.asKtType()
    }


    override fun buildSelfClassType(symbol: KtClassOrObjectSymbol): KtType {
        require(symbol is KtFirClassOrObjectSymbol)
        val type = symbol.firRef.withFir(FirResolvePhase.SUPER_TYPES) { firClass ->
            ConeClassLikeTypeImpl(
                firClass.symbol.toLookupTag(),
                firClass.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), isNullable = false) }.toTypedArray(),
                isNullable = false
            )
        }
        return type.asKtType()
    }
}

