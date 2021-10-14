/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KtStarProjectionTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.diagnostics.KT_DIAGNOSTIC_CONVERTER
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.convertVariance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal interface KtFirAnalysisSessionComponent {
    val analysisSession: KtFirAnalysisSession

    val rootModuleSession: FirSession get() = analysisSession.firResolveState.rootModuleSession
    val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
    val firSymbolBuilder get() = analysisSession.firSymbolBuilder
    val firResolveState get() = analysisSession.firResolveState

    fun ConeKotlinType.asKtType() = analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)

    fun FirPsiDiagnostic.asKtDiagnostic(): KtDiagnosticWithPsi<*> =
        KT_DIAGNOSTIC_CONVERTER.convert(analysisSession, this as FirDiagnostic)

    fun ConeDiagnostic.asKtDiagnostic(
        source: FirSourceElement,
        qualifiedAccessSource: FirSourceElement?,
        diagnosticCache: MutableList<FirDiagnostic>
    ): KtDiagnosticWithPsi<*>? {
        val firDiagnostic = toFirDiagnostics(analysisSession.rootModuleSession, source, qualifiedAccessSource).firstOrNull() ?: return null
        diagnosticCache += firDiagnostic
        check(firDiagnostic is FirPsiDiagnostic)
        return firDiagnostic.asKtDiagnostic()
    }

    val KtType.coneType: ConeKotlinType
        get() {
            require(this is KtFirType)
            return coneType
        }

    val KtTypeArgument.coneTypeProjection: ConeTypeProjection
        get() = when (this) {
            is KtStarProjectionTypeArgument -> ConeStarProjection
            is KtTypeArgumentWithVariance -> {
                typeContext.createTypeArgument(type.coneType, variance.convertVariance()) as ConeTypeProjection
            }
        }

    fun createTypeCheckerContext(): TypeCheckerState {
        // TODO use correct session here,
        return analysisSession.firResolveState.rootModuleSession.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = true
        )
    }

    fun FirQualifiedAccessExpression.createSubstitutorFromTypeArguments(): KtSubstitutor? {
        val symbol = when (val calleeReference = calleeReference) {
            is FirResolvedNamedReference -> calleeReference.resolvedSymbol as? FirCallableSymbol<*>
            is FirErrorNamedReference -> calleeReference.candidateSymbol as? FirCallableSymbol<*>
            else -> null
        } ?: return null
        return createSubstitutorFromTypeArguments(symbol)
    }

    fun FirQualifiedAccessExpression.createSubstitutorFromTypeArguments(functionSymbol: FirCallableSymbol<*>): KtSubstitutor {
        val typeArgumentMap = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        for (i in typeArguments.indices) {
            val type = typeArguments[i].safeAs<FirTypeProjectionWithVariance>()?.typeRef?.coneType
            if (type != null) {
                typeArgumentMap[functionSymbol.typeParameterSymbols[i]] = type
            }
        }
        val coneSubstitutor = substitutorByMap(typeArgumentMap, rootModuleSession)
        return firSymbolBuilder.typeBuilder.buildSubstitutor(coneSubstitutor)
    }
}
